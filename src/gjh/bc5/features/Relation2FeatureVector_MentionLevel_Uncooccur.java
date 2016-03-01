package gjh.bc5.features;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import gjh.bc5.main.BC5Runner;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Coreference;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Token;

public class Relation2FeatureVector_MentionLevel_Uncooccur extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7073473659534326061L;

	public Relation2FeatureVector_MentionLevel_Uncooccur(Alphabet dataAlphbet,
			LabelAlphabet targetAlphabet) {
		super(dataAlphbet, targetAlphabet);

		File stopWordsFile = new File("english_stopwords.tsv");
		if (!stopWordsFile.exists())
			throw new IllegalStateException("Unknown stopwords file!");
	}

	public void makeFeatures_MentionsBOW(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseMentionBOW, boolean ifUseStopWordsList,
			boolean ifUsePOS, boolean ifUseLemma) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Set<String> stopWords = BC5Runner.getStopWords();

		String chemicalPrefix = "chemWord@@";
		String diseasePrefix = "disWord@@";
		String chemPOSPrefix = "chemPOS@@";
		String disPOSPrefix = "disPOS@@";
		String chemLemmaPrefix = "chemLemma@@";
		String disLemmaPrefix = "disLemma@@";

		List<Token> chemMentionTokens = chemMention.getTokens(ab);
		List<Token> disMentionTokens = disMention.getTokens(ab);
		Set<Token> chemTokens = new TreeSet<Token>(chemMentionTokens);
		Set<Token> disTokens = new TreeSet<Token>(disMentionTokens);
		Set<Token> selectedChemTokens = chemTokens;
		Set<Token> selectedDisTokens = disTokens;

		if (ifUseStopWordsList) {
			Set<Token> tmp = new TreeSet<Token>();
			for (Token t : selectedChemTokens) {
				if (!stopWords.contains(t.getText().toLowerCase())) {
					tmp.add(t);
				}
			}
			selectedChemTokens = tmp;

			tmp = new TreeSet<Token>();
			for (Token t : selectedDisTokens) {
				if (!stopWords.contains(t.getText().toLowerCase())) {
					tmp.add(t);
				}
			}
			selectedDisTokens = tmp;
		}
		if (ifUseMentionBOW) {// bag for words
			for (Token t : selectedChemTokens) {
				fvWords.add(chemicalPrefix + t.getText().toLowerCase());
				fvValues.add(1.0);
			}

			for (Token t : selectedDisTokens) {
				fvWords.add(diseasePrefix + t.getText().toLowerCase());
				fvValues.add(1.0);
			}
		}
		if (ifUsePOS) {// part of speech
			for (Token t : selectedChemTokens) {
				// fvWords.add(chemPOSPrefix + t.getPos().toLowerCase());
				fvWords.add(chemPOSPrefix + t.getStanfordPOS().toLowerCase());
				fvValues.add(1.0);
			}
			for (Token t : selectedDisTokens) {
				// fvWords.add(chemPOSPrefix + t.getPos().toLowerCase());
				fvWords.add(disPOSPrefix + t.getStanfordPOS().toLowerCase());
				fvValues.add(1.0);
			}
		}
		if (ifUseLemma) {// lemma
			for (Token t : selectedChemTokens) {
				fvWords.add(chemLemmaPrefix + t.getLemma().toLowerCase());
				fvValues.add(1.0);
			}
			for (Token t : selectedDisTokens) {
				fvWords.add(disLemmaPrefix + t.getLemma().toLowerCase());
				fvValues.add(1.0);
			}
		}

	}

	public void makeFeatures_SentenceDistance(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		int chemSentIndex = chemMention.getSentenceIndex();
		int disSentIndex = disMention.getSentenceIndex();

		String chemicalBeforeDiseaseDistancePrefix = "chemicalBeforeDiseaseDistance@@";
		String chemicalAfterDiseaseDistancePrefix = "chemicalAfterDiseaseDistance@@";

		if (chemSentIndex <= disSentIndex) {
			int distance = disSentIndex - chemSentIndex;
			fvWords.add(chemicalBeforeDiseaseDistancePrefix + distance);
			fvValues.add(1.0);
		} else {
			int distance = chemSentIndex - disSentIndex;
			fvWords.add(chemicalAfterDiseaseDistancePrefix + distance);
			fvValues.add(1.0);
		}
	}

	private int getMaximumCountOfTheMostFrequentChemicalConceptInDocument(
			Abstract ab) {
		Map<String, Coreference> conceptIDsMap = ab.getChemicalCoreferences();

		int maximumFrequency = 0;
		Map<String, Integer> ids_freq = new HashMap<String, Integer>();
		for (Map.Entry<String, Coreference> entry : conceptIDsMap.entrySet()) {
			String key = entry.getKey();
			Coreference value = entry.getValue();

			if (!key.equals("-1")) {
				int freq = value.getMentions().size();
				ids_freq.put(key, freq);

				if (maximumFrequency < freq) {
					maximumFrequency = freq;
				}
			}
		}

		return maximumFrequency;
	}

	private int getCountOfSpecifiedConceptInDocument(Abstract ab,
			String conceptID) {

		if (conceptID.equals("-1")) {
			throw new IllegalArgumentException(
					"The conceptID should not be \"-1\"!");
		}

		Map<String, Coreference> chemConceptIDsMap = ab
				.getChemicalCoreferences();
		Map<String, Coreference> disConceptIDsMap = ab.getDiseaseCoreferences();

		if (chemConceptIDsMap.containsKey(conceptID)) {
			return chemConceptIDsMap.get(conceptID).getMentions().size();
		} else if (disConceptIDsMap.containsKey(conceptID)) {
			return disConceptIDsMap.get(conceptID).getMentions().size();
		} else {
			throw new IllegalArgumentException(
					"The conceptID cannot be found in this document!");
		}
	}

	private int getMaximumCountOfTheMostFrequentChemicalConceptBeforeSpecialPosition(
			Abstract ab, int offsetInDocument) {
		if (offsetInDocument < 0) {
			throw new IllegalArgumentException(
					"The offset should be larger than 0!");
		}
		if (offsetInDocument > ab.getWholeText().length()) {
			throw new IllegalArgumentException(
					"The offset should be smaller than the abstract size!");
		}

		Map<String, Coreference> conceptIDsMap = ab.getChemicalCoreferences();

		int maximumFrequency = 0;
		Map<String, Integer> ids_freq = new HashMap<String, Integer>();
		for (Map.Entry<String, Coreference> entry : conceptIDsMap.entrySet()) {
			String key = entry.getKey();
			Coreference value = entry.getValue();

			if (!key.equals("-1")) {
				Set<Mention> mens = value.getMentions();
				int freq = 0;
				for (Mention m : mens) {
					if (m.getEndOffsetInDocument() < offsetInDocument) {
						freq++;
					}
				}
				ids_freq.put(key, freq);

				if (maximumFrequency < freq) {
					maximumFrequency = freq;
				}
			}
		}
		return maximumFrequency;
	}

	private int getCountOfSpecifiedConceptBeforeSpecialPosition(Abstract ab,
			String conceptID, int offsetInDocument) {

		if (conceptID.equals("-1")) {
			throw new IllegalArgumentException(
					"The conceptID should not be \"-1\"!");
		}

		Map<String, Coreference> chemConceptIDsMap = ab
				.getChemicalCoreferences();
		Map<String, Coreference> disConceptIDsMap = ab.getDiseaseCoreferences();

		if (chemConceptIDsMap.containsKey(conceptID)) {
			Set<Mention> mens = chemConceptIDsMap.get(conceptID).getMentions();
			int count = 0;
			for (Mention m : mens) {
				if (m.getEndOffsetInDocument() < offsetInDocument) {
					count++;
				}
			}
			return count;

		} else if (disConceptIDsMap.containsKey(conceptID)) {
			Set<Mention> mens = disConceptIDsMap.get(conceptID).getMentions();
			int count = 0;
			for (Mention m : mens) {
				if (m.getEndOffsetInDocument() < offsetInDocument) {
					count++;
				}
			}
			return count;

		} else {
			throw new IllegalArgumentException(
					"The conceptID cannot be found in this document!");
		}
	}

	public void makeFeatures_ChemicalFrequency(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();

		String chemicalFrequencyBeforeDiseasePrefix = "chemicalFrequencyBeforeDisease@@";
		String chemicalFrequencyInDocumentPrefix = "chemicalFrequencyInDocument@@";
		String chemicalIsTheMostBeforeDiseasePrefix = "chemicalFrequencyIsTheMostBeforeDisease@@True";
		String chemicalIsTheMostInDocumentPrefix = "chemicalFrequencyIsTheMostInDocument@@True";

		int chemFreqInDoc = getCountOfSpecifiedConceptInDocument(ab,
				chemMention.getConceptID());
		fvWords.add(chemicalFrequencyInDocumentPrefix + chemFreqInDoc);
		fvValues.add(1.0);

		int chemFreqBeforeDis = getCountOfSpecifiedConceptBeforeSpecialPosition(
				ab, chemMention.getConceptID(),
				disMention.getStartOffsetInDocument());
		fvWords.add(chemicalFrequencyBeforeDiseasePrefix + chemFreqBeforeDis);
		fvValues.add(1.0);

		int maxChemFreqInDoc = getMaximumCountOfTheMostFrequentChemicalConceptInDocument(
				ab);
		if (chemFreqInDoc == maxChemFreqInDoc) {
			fvWords.add(chemicalIsTheMostInDocumentPrefix);
			fvValues.add(1.0);
		}

		int maxChemFreqBeforeDis = getMaximumCountOfTheMostFrequentChemicalConceptBeforeSpecialPosition(
				ab, disMention.getStartOffsetInDocument());
		if (chemFreqBeforeDis == maxChemFreqBeforeDis) {
			fvWords.add(chemicalIsTheMostBeforeDiseasePrefix);
			fvValues.add(1.0);
		}
	}

	public void makeFeatures_EntityIsTheOnly(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Abstract ab = relationSource.getAbstract();

		String chemicalIsTheOnlyEntityInDocPrefix = "chemicalIsTheOnlyEntityInDoc@@True";
		String diseaseIsTheOnlyEntityInDocPrefix = "diseaseIsTheOnlyEntityInDoc@@True";

		Set<String> chemConceptIDs = ab.getChemicalConceptIDs();
		Set<String> disConceptIDs = ab.getDiseaseConceptIDs();

		if (chemConceptIDs != null && !chemConceptIDs.isEmpty()) {
			if (chemConceptIDs.size() == 1) {
				fvWords.add(chemicalIsTheOnlyEntityInDocPrefix);
				fvValues.add(1.0);
			}
		}

		if (disConceptIDs != null && !disConceptIDs.isEmpty()) {
			if (disConceptIDs.size() == 1) {
				fvWords.add(diseaseIsTheOnlyEntityInDocPrefix);
				fvValues.add(1.0);
			}
		}
	}

	public void makeFeatures_MentionPairInTheSameBlock(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Abstract a = source.getAbstract();
		String wholeText = a.getWholeText();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();

		Mention firstMention = null;
		Mention secondMention = null;
		if (chemMention.compareTo(disMention) < 0) {
			firstMention = chemMention;
			secondMention = disMention;
		} else {
			firstMention = disMention;
			secondMention = chemMention;
		}

		int sPos = firstMention.getEndOffsetInDocument();
		int lPos = secondMention.getStartOffsetInDocument();
		String subText = wholeText.substring(sPos, lPos);
		Pattern pattern = Pattern.compile("[A-Z]+:");
		Matcher matcher = pattern.matcher(subText);
		if (!matcher.find()) {
			String mentionPairInTheSameBlock = "mentionPairInTheSameBlock@@True";
			fvWords.add(mentionPairInTheSameBlock);
			fvValues.add(1.0);
		}
	}

	public void makeFeatures_MentionOccurInTitle(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Mention chemMention = source.getChemMention();
		int chemMentionSentIndex = chemMention.getSentenceIndex();
		Mention disMention = source.getDisMention();
		int disMentionSentIndex = disMention.getSentenceIndex();

		if (chemMentionSentIndex == 0) {
			String chemMentionOccurInTitle = "chemMentionOccurInTitle@@True";
			fvWords.add(chemMentionOccurInTitle);
			fvValues.add(1.0);
		}

		if (disMentionSentIndex == 0) {
			String disMentionOccurInTitle = "disMentionOccurInTitle@@True";
			fvWords.add(disMentionOccurInTitle);
			fvValues.add(1.0);
		}
	}

	// @Override
	public Instance pipe(Instance carrier) {
		MentionPairAsInstanceSource source = (MentionPairAsInstanceSource) carrier
				.getSource();
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();

		Alphabet dataAlphbet = getDataAlphabet();
		LabelAlphabet targetAlphabet = (LabelAlphabet) getTargetAlphabet();
		String documentID = relationSource.getDocumentID();
		// System.out.println(documentID);
		String chemicalConceptID = relationSource.getChemicalConceptID();
		String diseaseConceptID = relationSource.getDiseaseConceptID();

		String instanceName = documentID + "_" + chemicalConceptID + "_"
				+ chemMention.getSentenceIndex() + "_"
				+ chemMention.getStartOffsetInDocument() + "_"
				+ chemMention.getEndOffsetInDocument() + "_"
				+ chemMention.getText() + "_" + diseaseConceptID + "_"
				+ disMention.getSentenceIndex() + "_"
				+ disMention.getStartOffsetInDocument() + "_"
				+ disMention.getEndOffsetInDocument() + "_"
				+ disMention.getText();

		List<String> fvWords = new ArrayList<String>();
		List<Double> fvValues = new ArrayList<Double>();

		makeFeatures_MentionsBOW(source, fvWords, fvValues, true, false, true,
				true);// work
		makeFeatures_SentenceDistance(source, fvWords, fvValues);// work
		makeFeatures_ChemicalFrequency(source, fvWords, fvValues);// work
		makeFeatures_MentionPairInTheSameBlock(source, fvWords, fvValues);// work
		makeFeatures_EntityIsTheOnly(source, fvWords, fvValues);// work

		String[] keys = new String[fvWords.size()];
		keys = fvWords.toArray(keys);
		double[] values = new double[fvWords.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = 1.0;
		}

		FeatureVector fv = new FeatureVector(dataAlphbet, keys, values);

		Label lb = targetAlphabet
				.lookupLabel(relationSource.getRelationType().toString());

		carrier.setName(instanceName);
		carrier.setData(fv);
		carrier.setTarget(lb);
		carrier.setSource(source);

		return carrier;
	}
}
