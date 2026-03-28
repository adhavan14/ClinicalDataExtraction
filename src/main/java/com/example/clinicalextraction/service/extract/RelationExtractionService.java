package com.example.clinicalextraction.service.extract;

import com.example.clinicalextraction.dto.Entity;
import com.example.clinicalextraction.entity.*;
import com.example.clinicalextraction.service.ClinicalXMLHandler;
import com.example.clinicalextraction.service.XmlParser;
import com.example.clinicalextraction.util.KeywordUtil;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelationExtractionService {

    private final XmlParser xmlParser;

    private final SAXParser saxParser;

    private static final StanfordCoreNLP pipeline;

    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse");
        pipeline = new StanfordCoreNLP(props);
    }

    public RelationExtractionService(XmlParser xmlParser) throws ParserConfigurationException, SAXException {
        this.xmlParser = xmlParser;
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParser = saxParserFactory.newSAXParser();
    }


    public ClinicalEvidence process(String filePath) throws IOException, SAXException {
        String xml = Files.readString(Paths.get(filePath));

        List<String> chunks = xmlParser.chunkXml(xml);

        List<SentenceData> sentenceDataList = new ArrayList<>();

        Set<CandidatePair> candidatePairs = new HashSet<>();

        for (String chunk :  chunks) {
            sentenceDataList.add(parseXml(chunk));
        }

        findDistance(sentenceDataList, candidatePairs);

        for(CandidatePair candidatePair : candidatePairs) {
            List<SentenceData> sentenceData = getRelevantSentences(sentenceDataList, candidatePair);
            String sentence = sentenceData.stream().map(SentenceData::getSentence).collect(Collectors.joining(" "));
            findDependencyLink(sentence, candidatePair);
        }

        return ClinicalEvidence.builder()
                .sentenceDataList(sentenceDataList)
                .candidatePairs(candidatePairs)
                .relationEvidences(buildTreatRelations(sentenceDataList, candidatePairs))
                .build();
    }

    private List<RelationEvidence> buildTreatRelations(List<SentenceData> sentenceDataList, Set<CandidatePair> candidatePairs) {
        List<RelationEvidence> results = new ArrayList<>();

        for (CandidatePair pair : candidatePairs) {

            double score = 0.0;
            List<String> keywords = new ArrayList<>();
            if (pair.getProximityLevel().equals("SAME")) {
                String sentence = sentenceDataList.get(pair.getDrugSentenceIndex()).getSentence();

                for (String lemma : pair.getSentenceLemmas()) {
                    if (KeywordUtil.ADVERSE_EFFECT.contains(lemma)) {
                        keywords.clear();
                        continue;
                    }

                    if (KeywordUtil.TREAT_KEYWORDS.containsKey(lemma)) {
                        score += KeywordUtil.getWeight(lemma);
                        keywords.add(lemma);
                    }


                }

                if (sentence.contains(pair.getCondition()) && sentence.contains(pair.getDrug())) {
                    score += 0.5;
                }
            }

            if (pair.getProximityLevel().equals("ADJACENT")) {

                for (String lemma : pair.getSentenceLemmas()) {
                    if (KeywordUtil.ADVERSE_EFFECT.contains(lemma)) {
                        keywords.clear();
                        continue;
                    }

                    if (KeywordUtil.TREAT_KEYWORDS.containsKey(lemma)) {
                        score += KeywordUtil.getWeight(lemma);
                        keywords.add(lemma);
                    }
                }

                List<String> compounds = pair.getDependencyLinks().stream()
                        .filter(dependencyLink -> dependencyLink.getRelation().equals("compound"))
                        .map(DependencyLink::getTarget)
                        .toList();

                if (compounds.contains(pair.getCondition()) && compounds.contains(pair.getDrug())) {
                    score += 0.5;
                }
            }
            score -= pair.getDistance() * 0.1;

            if (score > 1 && !keywords.isEmpty()) {
                results.add(RelationEvidence.builder()
                        .drug(pair.getDrug())
                        .condition(pair.getCondition())
                        .relationType("treats")
                        .confidence(score)
                        .keywords(keywords)
                        .dependencyPaths(List.of("dependency_connection"))
                        .distance(pair.getDistance())
                        .proximity(pair.getProximityLevel())
                        .build());
            }
        }

        return results;
    }

    private void findDependencyLink(String text, CandidatePair candidatePair) {

        List<DependencyLink> dependencyLinks = new ArrayList<>();

        List<String> lemmas = new ArrayList<>();

        System.out.println(text);

        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);

        for (CoreSentence sentence : doc.sentences()) {
            SemanticGraph graph = sentence.dependencyParse();

            for (CoreLabel token : sentence.tokens()) {
                lemmas.add(token.lemma().toLowerCase());
            }

            for (SemanticGraphEdge edge : graph.edgeListSorted()) {
                String gov = edge.getGovernor().lemma();
                String dep = edge.getDependent().lemma();
                String rel = edge.getRelation().toString();

                dependencyLinks.add(DependencyLink.builder()
                                .source(gov)
                                .relation(rel)
                                .target(dep)
                        .build());
            }
        }
        candidatePair.setDependencyLinks(dependencyLinks);
        candidatePair.setSentenceLemmas(lemmas);
    }

    private void findDistance(List<SentenceData> sentenceData, Set<CandidatePair> candidatePairs) {

        int size = sentenceData.size();

        for (int i = 0; i< size; i++) {
            generateDistance(sentenceData.get(i), sentenceData.get(i), i, i, candidatePairs);
            if (i>0) {
                generateDistance(sentenceData.get(i-1), sentenceData.get(i), i-1, i, candidatePairs);
            } else if (i < sentenceData.size()-1) {
                generateDistance(sentenceData.get(i), sentenceData.get(i+1), i, i+1, candidatePairs);
            }
        }
    }

    private void generateDistance(SentenceData dataOne, SentenceData dataTwo, int drugIndex, int conditionIndex, Set<CandidatePair> candidatePairs) {
        for (String drug : dataOne.getDrugs()) {
            for (String condition : dataTwo.getConditions()) {
                int distance = Math.abs(drugIndex - conditionIndex);
                CandidatePair candidatePair = CandidatePair.builder()
                        .drug(drug)
                        .condition(condition)
                        .drugSentenceIndex(drugIndex)
                        .conditionSentenceIndex(conditionIndex)
                        .distance(distance)
                        .proximityLevel(getProximityLevel(distance))
                        .build();
                candidatePairs.add(candidatePair);
            }
        }

        for (String drug : dataTwo.getDrugs()) {
            for (String condition : dataOne.getConditions()) {
                int distance = Math.abs(drugIndex - conditionIndex);
                CandidatePair candidatePair = CandidatePair.builder()
                        .drug(drug)
                        .condition(condition)
                        .drugSentenceIndex(drugIndex)
                        .conditionSentenceIndex(conditionIndex)
                        .distance(distance)
                        .proximityLevel(getProximityLevel(distance))
                        .build();
                candidatePairs.add(candidatePair);
            }
        }
    }

    private String getProximityLevel(int distance) {
        if (distance == 0) {
            return "SAME";
        }
        else if (distance==1) {
            return "ADJACENT";
        } else {
            return "DISTANT";
        }
    }

    private SentenceData parseXml(String chunk) throws IOException, SAXException {

        ClinicalXMLHandler clinicalXMLHandler = new ClinicalXMLHandler();

        String wrappedChunk = "<root>" + chunk + "</root>";

        saxParser.parse(new InputSource(new StringReader(wrappedChunk)), clinicalXMLHandler);

        return SentenceData.builder()
                .sentence(chunk.replaceAll("<[^>]+>", "").trim())
                .conditions(getConditions(clinicalXMLHandler.entities))
                .drugs(getDrugs(clinicalXMLHandler.entities))
                .build();
    }


    private List<String> getConditions(List<Entity> entities) {
        return entities.stream()
                .filter(entity -> entity.getTag().equals("condition"))
                .map(Entity::getText)
                .toList();
    }

    private List<String> getDrugs(List<Entity> entities) {
        return entities.stream()
                .filter(entity -> entity.getTag().equals("drug"))
                .map(Entity::getText)
                .toList();
    }

    private List<SentenceData> getRelevantSentences(
            List<SentenceData> sentenceDataList,
            CandidatePair pair) {

        List<SentenceData> result = new ArrayList<>();

        if (pair.getProximityLevel().equals("SAME")) {
            result.add(sentenceDataList.get(pair.getDrugSentenceIndex()));
        }
        else if (pair.getProximityLevel().equals("ADJACENT")) {
            result.add(sentenceDataList.get(pair.getDrugSentenceIndex()));
            result.add(sentenceDataList.get(pair.getConditionSentenceIndex()));
        }

        return result;
    }


}
