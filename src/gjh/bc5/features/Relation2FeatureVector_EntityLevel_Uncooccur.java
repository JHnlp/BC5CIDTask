package gjh.bc5.features;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Coreference;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Token;

// instance source is a "EntityPairIsInstanceSource", not a "Relation"
public class Relation2FeatureVector_EntityLevel_Uncooccur extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2768286091083795205L;
	private Set<String> stopWords;
	private static final int NGram_Window_Size = 3;

	public Relation2FeatureVector_EntityLevel_Uncooccur(Alphabet dataAlphbet,
			LabelAlphabet targetAlphabet) {
		super(dataAlphbet, targetAlphabet);

		File stopWordsFile = new File("english_stopwords.tsv");
		if (!stopWordsFile.exists())
			throw new IllegalStateException("Unknown stopwords file!");

		stopWords = new HashSet<String>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(stopWordsFile, "utf-8");
				while (in.hasNextLine()) {
					String word = in.nextLine();
					// System.out.println(word);
					if (word != null && !word.isEmpty())
						stopWords.add(word);
				}

			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
	}

	public void makeFeatures_MentionsBOW(EntityPairIsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseMentionBOW, boolean ifUseStopWordsList,
			boolean ifUsePOS, boolean ifUseLemma) {
		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		Relation relationSource = source.getRelationSource();
		Abstract ab = relationSource.getAbstract();

		String chemicalPrefix = "chemWord@@";
		String diseasePrefix = "disWord@@";
		String chemPOSPrefix = "chemPOS@@";
		String disPOSPrefix = "disPOS@@";
		String chemLemmaPrefix = "chemLemma@@";
		String disLemmaPrefix = "disLemma@@";

		Set<Mention> chemicalMentions = source.getUsedChemicalMentions();
		Set<Token> chemTokens = new TreeSet<Token>();
		for (Mention m : chemicalMentions)
			chemTokens.addAll(m.getTokens(ab));

		Set<Mention> diseaseMentions = source.getUsedDiseaseMentions();
		Set<Token> disTokens = new TreeSet<Token>();
		for (Mention m : diseaseMentions)
			disTokens.addAll(m.getTokens(ab));

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

	public void makeFeatures_LeftNGram(EntityPairIsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseBOW, boolean ifUseStopWordsList, boolean ifUsePOS,
			boolean ifUseLemma) {
		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		for (MentionPairAsInstanceSource mp : menPairs) {
			Mention chemMention = mp.getChemMention();
			Mention disMention = mp.getDisMention();

			Relation relationSource = source.getRelationSource();
			Abstract ab = relationSource.getAbstract();
			Mention formerMention = null;
			if (chemMention.compareTo(disMention) < 0) {
				formerMention = chemMention;
			} else {
				formerMention = disMention;
			}

			String mentionLeftWordPrefix = "menLeftNGramWord";
			String mentionLeftPOSPrefix = "menLeftNGramPOS";
			String mentionLeftLemmaPrefix = "menLeftNGramLemma";
			String mentionMidPrefix = "@@";

			List<Token> leftTokens = formerMention
					.getLeftNGramTokensOfMentionWithoutPuctuations(ab,
							NGram_Window_Size);

			Set<Token> sortedTokens = new TreeSet<Token>(leftTokens);

			if (ifUseStopWordsList) {
				Set<Token> tmp = new TreeSet<Token>();
				for (Token t : sortedTokens) {
					if (!stopWords.contains(t.getText().toLowerCase())) {
						tmp.add(t);
					}
				}
				sortedTokens = tmp;
			}

			List<Token> selectedTokens = new ArrayList<Token>(sortedTokens);

			for (int i = 0; i < selectedTokens.size(); i++) {
				int num = i + 1;
				String menLeftNGramWordPrefix = mentionLeftWordPrefix + num
						+ mentionMidPrefix;
				String menLeftNGramPOSPrefix = mentionLeftPOSPrefix + num
						+ mentionMidPrefix;
				String menLeftNGramLemmaPrefix = mentionLeftLemmaPrefix + num
						+ mentionMidPrefix;
				Token t = selectedTokens.get(i);

				if (ifUseBOW) {// bag of words
					fvWords.add(
							menLeftNGramWordPrefix + t.getText().toLowerCase());
					fvValues.add(1.0);
				}

				if (ifUsePOS) {// part of speech
					fvWords.add(menLeftNGramPOSPrefix
							+ t.getStanfordPOS().toLowerCase());
					// fvWords.add(menLeftNGramPOSPrefix
					// + t.getPos().toLowerCase());
					fvValues.add(1.0);
				}

				if (ifUseLemma) {// lemma
					fvWords.add(menLeftNGramLemmaPrefix
							+ t.getLemma().toLowerCase());
					fvValues.add(1.0);
				}
			}
		}
	}

	public void makeFeatures_SentenceDistance(EntityPairIsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {
		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		String chemicalBeforeDiseaseDistancePrefix = "chemicalBeforeDiseaseDistance@@";
		String chemicalAfterDiseaseDistancePrefix = "chemicalAfterDiseaseDistance@@";

		for (MentionPairAsInstanceSource m : menPairs) {
			Mention chemMention = m.getChemMention();
			int chemSentIndex = chemMention.getSentenceIndex();
			Mention disMention = m.getDisMention();
			int disSentIndex = disMention.getSentenceIndex();

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
			EntityPairIsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		Relation relation = source.getRelationSource();
		Abstract ab = relation.getAbstract();
		String chemicalFrequencyBeforeDiseasePrefix = "chemicalFrequencyBeforeDisease@@";
		String chemicalFrequencyInDocumentPrefix = "chemicalFrequencyInDocument@@";
		String chemicalIsTheMostBeforeDiseasePrefix = "chemicalFrequencyIsTheMostBeforeDisease@@True";
		String chemicalIsTheMostInDocumentPrefix = "chemicalFrequencyIsTheMostInDocument@@True";

		for (MentionPairAsInstanceSource m : menPairs) {
			Mention chemMention = m.getChemMention();
			Mention disMention = m.getDisMention();

			int chemFreqInDoc = getCountOfSpecifiedConceptInDocument(ab,
					chemMention.getConceptID());
			fvWords.add(chemicalFrequencyInDocumentPrefix + chemFreqInDoc);
			fvValues.add(1.0);

			int chemFreqBeforeDis = getCountOfSpecifiedConceptBeforeSpecialPosition(
					ab, chemMention.getConceptID(),
					disMention.getStartOffsetInDocument());
			fvWords.add(
					chemicalFrequencyBeforeDiseasePrefix + chemFreqBeforeDis);
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
	}

	// @Override
	public Instance pipe(Instance carrier) {
		EntityPairIsInstanceSource source = (EntityPairIsInstanceSource) carrier
				.getSource();
		Relation relationSource = source.getRelationSource();
		Alphabet dataAlphbet = getDataAlphabet();
		LabelAlphabet targetAlphabet = (LabelAlphabet) getTargetAlphabet();
		String documentID = relationSource.getDocumentID();
		// System.out.println(documentID);
		String chemicalConceptID = relationSource.getChemicalConceptID();
		String diseaseConceptID = relationSource.getDiseaseConceptID();

		String instanceName = documentID + "-" + chemicalConceptID + "-"
				+ diseaseConceptID;

		List<String> fvWords = new ArrayList<String>();
		List<Double> fvValues = new ArrayList<Double>();

		makeFeatures_MentionsBOW(source, fvWords, fvValues, true, false, true,
				false);
		makeFeatures_SentenceDistance(source, fvWords, fvValues);
		makeFeatures_ChemicalFrequency(source, fvWords, fvValues);

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
