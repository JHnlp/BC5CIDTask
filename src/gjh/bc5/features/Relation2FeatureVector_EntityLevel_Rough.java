package gjh.bc5.features;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;

public class Relation2FeatureVector_EntityLevel_Rough extends Pipe {

	private static final long serialVersionUID = -389473912759786259L;
	private Set<String> stopWords;

	public Relation2FeatureVector_EntityLevel_Rough(Alphabet dataAlphbet,
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

	public void makeFeaturesWithRelationMentions(Relation source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseMentionBOW, boolean ifUseStopWordsList,
			boolean ifUsePOS, boolean ifUseLemma) {

		Relation relationSource = source;
		Abstract ab = relationSource.getAbstract();

		String chemicalPrefix = "chemWord@@";
		String diseasePrefix = "disWord@@";
		String chemPOSPrefix = "chemPOS@@";
		String disPOSPrefix = "disPOS@@";
		String chemLemmaPrefix = "chemLemma@@";
		String disLemmaPrefix = "disLemma@@";
		Set<Mention> chemicalMentions = relationSource.getChemicalMentions();

		for (Mention m : chemicalMentions) {
			List<gjh.bc5.utils.Token> tokens = m.getTokens(ab);
			for (gjh.bc5.utils.Token t : tokens) {
				if (ifUseMentionBOW) {// bag fo words
					if (ifUseStopWordsList) {

						if (stopWords.contains(t.getText().toLowerCase())) {
							continue;
						} else {
							// bag of words
							fvWords.add(
									chemicalPrefix + t.getText().toLowerCase());
							fvValues.add(1.0);

							if (ifUsePOS) {// part of speech
								// fvWords.add(chemPOSPrefix
								// + t.getPos().toLowerCase());
								fvWords.add(chemPOSPrefix
										+ t.getStanfordPOS().toLowerCase());
								fvValues.add(1.0);
							}
							if (ifUseLemma) {// lemma
								fvWords.add(chemLemmaPrefix
										+ t.getLemma().toLowerCase());
								fvValues.add(1.0);
							}
						}
					} else {
						// bag of words
						fvWords.add(chemicalPrefix + t.getText().toLowerCase());
						fvValues.add(1.0);

						if (ifUsePOS) {// part of speech
							fvWords.add(
									chemPOSPrefix + t.getPos().toLowerCase());
							fvWords.add(chemPOSPrefix
									+ t.getStanfordPOS().toLowerCase());
							fvValues.add(1.0);
						}
						if (ifUseLemma) {// lemma
							fvWords.add(chemLemmaPrefix
									+ t.getLemma().toLowerCase());
							fvValues.add(1.0);
						}
					}
				}
			}
		}
		Set<Mention> diseaseMentions = relationSource.getDiseaseMentions();
		for (Mention m : diseaseMentions) {
			List<gjh.bc5.utils.Token> tokens = m.getTokens(ab);
			for (gjh.bc5.utils.Token t : tokens) {
				if (ifUseMentionBOW) {// bag of words
					if (ifUseStopWordsList) {

						if (stopWords.contains(t.getText().toLowerCase())) {
							continue;

						} else {
							fvWords.add(
									diseasePrefix + t.getText().toLowerCase());
							fvValues.add(1.0);

							if (ifUsePOS) {// part of speech
								// fvWords.add(disPOSPrefix
								// + t.getPos().toLowerCase());
								fvWords.add(disPOSPrefix
										+ t.getStanfordPOS().toLowerCase());
								fvValues.add(1.0);
							}
							if (ifUseLemma) {// lemma
								fvWords.add(disLemmaPrefix
										+ t.getLemma().toLowerCase());
								fvValues.add(1.0);
							}
						}
					} else {
						fvWords.add(diseasePrefix + t.getText().toLowerCase());
						fvValues.add(1.0);

						if (ifUsePOS) {// part of speech
							// fvWords.add(disPOSPrefix +
							// t.getPos().toLowerCase());
							fvWords.add(disPOSPrefix
									+ t.getStanfordPOS().toLowerCase());
							fvValues.add(1.0);
						}
						if (ifUseLemma) {// lemma
							fvWords.add(disLemmaPrefix
									+ t.getLemma().toLowerCase());
							fvValues.add(1.0);
						}
					}
				}
			}
		}
	}

	public void makeFeaturesWithTokensFromBetweenTheNearestRelationMentions(
			EntityPairIsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues, boolean ifUseTokenBOW,
			boolean ifUseStopWordsList, boolean ifUsePOS, boolean ifUseLemma) {

		Relation relationSource = source.getRelationSource();

		String middleWordPrefix = "middleWord@@";
		String midPOSPrefix = "middlePOS@@";
		String midLemmaPrefix = "middleLemma@@";
		List<gjh.bc5.utils.Token> middleTokens = relationSource
				.getTokensBetweenTwoNearestMentions();
		for (gjh.bc5.utils.Token t : middleTokens) {
			if (ifUseTokenBOW) {// bag of words
				if (ifUseStopWordsList) {// use stop words list
					if (stopWords.contains(t.getText())) {
						continue;
					} else {
						// bag of words
						fvWords.add(
								middleWordPrefix + t.getText().toLowerCase());
						fvValues.add(1.0);

						if (ifUsePOS) {// part of speech
							fvWords.add(
									midPOSPrefix + t.getPos().toLowerCase());
							fvValues.add(1.0);
						}

						if (ifUseLemma) {// lemma
							fvWords.add(midLemmaPrefix
									+ t.getLemma().toLowerCase());
							fvValues.add(1.0);
						}
					}
				} else {
					fvWords.add(middleWordPrefix + t.getText().toLowerCase());
					fvValues.add(1.0);

					if (ifUsePOS) {
						fvWords.add(midPOSPrefix + t.getPos().toLowerCase());
						fvValues.add(1.0);
					}
					if (ifUseLemma) {
						fvWords.add(
								midLemmaPrefix + t.getLemma().toLowerCase());
						fvValues.add(1.0);
					}
				}
			}
		}
	}

	public void makeFeaturesCanRelationMentionsOccurInOneSentence(
			EntityPairIsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();

		String canBothMentionsInSameSentence = "inSameSentence@@";
		if (relationSource.hasCooccurrenceInOneSentence()) {
			fvWords.add(canBothMentionsInSameSentence);
			fvValues.add(1.0);
		}
	}

	// @Override
	public Instance pipe(Instance carrier) {
		Relation relationSource = (Relation) carrier.getSource();
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

		makeFeaturesWithRelationMentions(relationSource, fvWords, fvValues,
				true, false, true, true);

		String[] keys = new String[fvWords.size()];
		keys = fvWords.toArray(keys);
		double[] values = new double[fvWords.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = 1.0;
		}

		FeatureVector fv = new FeatureVector(dataAlphbet, keys, values);
		// AugmentableFeatureVector afv = new
		// AugmentableFeatureVector(dataAlphbet, pl, false);
		// afv.add (FeatureVector fv, String prefix, boolean binary);
		// afv.toFeatureVector ();

		Label lb = targetAlphabet
				.lookupLabel(relationSource.getRelationType().toString());

		carrier.setName(instanceName);
		carrier.setData(fv);
		carrier.setTarget(lb);
		carrier.setSource(relationSource);

		return carrier;
	}

}
