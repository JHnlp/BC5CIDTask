package gjh.bc5.features;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Sentence;
import gjh.bc5.utils.Token;

public class Relation2FeatureVector_EntityLevel_Cooccur extends Pipe {

	private static final long serialVersionUID = -8872902997569108509L;
	private Set<String> stopWords;
	private static final int NGram_Window_Size = 3;

	public Relation2FeatureVector_EntityLevel_Cooccur(Alphabet dataAlphbet,
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

	private boolean isPunctuationExcludingHyphen(char ch) {
		return ("`~!@#$%^&*()–=_+[]\\{}|;':\",./<>?".indexOf(ch) != -1);
	}

	private boolean isPunctuationExcludingHyphen(String str) {
		if (str.length() == 1) {
			char ch = str.charAt(0);
			return isPunctuationExcludingHyphen(ch);
		} else {
			return false;
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

	public void makeFeatures_MentionsBOW2(EntityPairIsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {
		boolean ifUseMentionBOW = false;
		// boolean ifUseStopWordsList = false;
		boolean ifUsePOS = false;
		boolean ifUseLemma = true;

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
		Set<Mention> diseaseMentions = source.getUsedDiseaseMentions();

		if (ifUseMentionBOW) {// bag for words
			for (Mention m : chemicalMentions) {
				List<Token> tokens = m.getTokens(ab);
				String mentionWord = "";
				for (int i = 0; i < tokens.size(); i++) {
					if (i < tokens.size() - 1) {
						mentionWord += tokens.get(i).getText().toLowerCase()
								+ "_";
					} else {
						mentionWord += tokens.get(i).getText().toLowerCase();
					}
				}
				fvWords.add(chemicalPrefix + mentionWord);
				fvValues.add(1.0);
			}
			for (Mention m : diseaseMentions) {
				List<Token> tokens = m.getTokens(ab);
				String mentionWord = "";
				for (int i = 0; i < tokens.size(); i++) {
					if (i < tokens.size() - 1) {
						mentionWord += tokens.get(i).getText().toLowerCase()
								+ "_";
					} else {
						mentionWord += tokens.get(i).getText().toLowerCase();
					}
				}
				fvWords.add(diseasePrefix + mentionWord);
				fvValues.add(1.0);
			}
		}
		if (ifUsePOS) {// part of speech
			for (Mention m : chemicalMentions) {
				List<Token> tokens = m.getTokens(ab);
				String mentionPOS = "";
				for (int i = 0; i < tokens.size(); i++) {
					if (i < tokens.size() - 1) {
						mentionPOS += tokens.get(i).getStanfordPOS() + "_";
					} else {
						mentionPOS += tokens.get(i).getStanfordPOS();
					}
				}
				fvWords.add(chemPOSPrefix + mentionPOS);
				fvValues.add(1.0);
			}

			for (Mention m : diseaseMentions) {
				List<Token> tokens = m.getTokens(ab);
				String mentionPOS = "";
				for (int i = 0; i < tokens.size(); i++) {
					if (i < tokens.size() - 1) {
						mentionPOS += tokens.get(i).getStanfordPOS() + "_";
					} else {
						mentionPOS += tokens.get(i).getStanfordPOS();
					}
				}
				fvWords.add(disPOSPrefix + mentionPOS);
				fvValues.add(1.0);
			}
		}
		if (ifUseLemma) {// lemma
			for (Mention m : chemicalMentions) {
				List<Token> tokens = m.getTokens(ab);
				String mentionLemma = "";
				for (int i = 0; i < tokens.size(); i++) {
					if (i < tokens.size() - 1) {
						mentionLemma += tokens.get(i).getLemma() + "_";
					} else {
						mentionLemma += tokens.get(i).getLemma();
					}
				}
				fvWords.add(chemLemmaPrefix + mentionLemma);
				fvValues.add(1.0);
			}
			for (Mention m : diseaseMentions) {
				List<Token> tokens = m.getTokens(ab);
				String mentionLemma = "";
				for (int i = 0; i < tokens.size(); i++) {
					if (i < tokens.size() - 1) {
						mentionLemma += tokens.get(i).getLemma() + "_";
					} else {
						mentionLemma += tokens.get(i).getLemma();
					}
				}
				fvWords.add(disLemmaPrefix + mentionLemma);
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

	private List<Token> filterTokensInParenthesis(List<Token> tokens) {
		List<Token> afterFilterTokens = new ArrayList<Token>();
		Set<Token> tokensInParenthesis = new HashSet<Token>();
		Stack<Token> stack_parenthesis = new Stack<Token>();
		Stack<Integer> stack_index = new Stack<Integer>();

		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.get(i).equals("(")) {
				stack_parenthesis.push(tokens.get(i));
				stack_index.push(i);
			}

			if (tokens.get(i).equals(")")) {
				if (!stack_parenthesis.isEmpty()) {
					int startIndex = stack_index.pop();
					int endIndex = i;

					for (int j = startIndex; j <= endIndex; j++)
						tokensInParenthesis.add(tokens.get(j));
				}
			}
		}

		for (int i = 0; i < tokens.size(); i++) {
			if (!tokensInParenthesis.contains(tokens.get(i)))
				afterFilterTokens.add(tokens.get(i));
		}

		return afterFilterTokens;
	}

	public void makeFeatures_RightNGram(EntityPairIsInstanceSource source,
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
			Mention latterMention = null;
			if (chemMention.compareTo(disMention) < 0) {
				latterMention = disMention;
			} else {
				latterMention = chemMention;
			}

			String mentionRightWordPrefix = "menRightNGramWord";
			String mentionRightPOSPrefix = "menRightNGramPOS";
			String mentionRightLemmaPrefix = "menRightNGramLemma";
			String mentionMidPrefix = "@@";

			List<Token> rightTokens = latterMention
					.getRightNGramTokensOfMentionWithoutPuctuations(ab,
							NGram_Window_Size);

			Set<Token> sortedTokens = new TreeSet<Token>(rightTokens);

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
				String menRightNGramWordPrefix = mentionRightWordPrefix + num
						+ mentionMidPrefix;
				String menRightNGramPOSPrefix = mentionRightPOSPrefix + num
						+ mentionMidPrefix;
				String menRightNGramLemmaPrefix = mentionRightLemmaPrefix + num
						+ mentionMidPrefix;
				Token t = selectedTokens.get(i);

				if (ifUseBOW) {// bag of words
					fvWords.add(menRightNGramWordPrefix
							+ t.getText().toLowerCase());
					fvValues.add(1.0);
				}

				if (ifUsePOS) {// part of speech
					fvWords.add(menRightNGramPOSPrefix
							+ t.getStanfordPOS().toLowerCase());
					// fvWords.add(menRightNGramPOSPrefix
					// + t.getPos().toLowerCase());
					fvValues.add(1.0);
				}

				if (ifUseLemma) {// lemma
					fvWords.add(menRightNGramLemmaPrefix
							+ t.getLemma().toLowerCase());
					fvValues.add(1.0);
				}
			}
		}
	}

	// 判断动词之前是否还有其他mention
	private boolean hasAnyOtherMentionBeforeTheToken(Mention formerMention,
			Abstract ab, Token verbToken) {
		if (!formerMention.getDocumentID().equals(ab.getDocumentID())
				|| !formerMention.getDocumentID()
						.equals(verbToken.getDocumentID())
				|| formerMention.getSentenceIndex() != verbToken
						.getSentenceIndex())
			throw new IllegalArgumentException("The arguments are wrong!");

		int mentionEndOffsetInSentence = formerMention
				.getEndOffsetInSentence(ab);
		int tokenStartOffsetInSentence = verbToken.getStartOffInSentence();
		if (tokenStartOffsetInSentence < mentionEndOffsetInSentence)
			throw new IllegalArgumentException(
					"The mention offset should be ahead of the token offset in the sentence!");

		Sentence sentence = formerMention.getSentence(ab);
		List<Mention> mentionsInSentence = sentence.getMentions();
		for (Mention m : mentionsInSentence) {
			if (m.getStartOffsetInSentence(
					sentence) >= mentionEndOffsetInSentence
					&& m.getEndOffsetInSentence(
							sentence) <= tokenStartOffsetInSentence) {
				return true;
			}
		}
		return false;
	}

	// 判断动词之后是否还有其他mention
	private boolean hasAnyOtherMentionAfterTheToken(Mention latterMention,
			Abstract ab, Token verbToken) {
		if (!latterMention.getDocumentID().equals(ab.getDocumentID())
				|| !latterMention.getDocumentID()
						.equals(verbToken.getDocumentID())
				|| latterMention.getSentenceIndex() != verbToken
						.getSentenceIndex())
			throw new IllegalArgumentException("The arguments are wrong!");

		int mentionStartOffsetInSentence = latterMention
				.getStartOffsetInSentence(ab);
		int tokenEndOffsetInSentence = verbToken.getEndOffInSentence();
		if (mentionStartOffsetInSentence < tokenEndOffsetInSentence)
			throw new IllegalArgumentException(
					"The mention offset should be after  the token offset in the sentence!");

		Sentence sentence = latterMention.getSentence(ab);
		List<Mention> mentionsInSentence = sentence.getMentions();
		for (Mention m : mentionsInSentence) {
			if (m.getStartOffsetInSentence(sentence) >= tokenEndOffsetInSentence
					&& m.getEndOffsetInSentence(
							sentence) <= mentionStartOffsetInSentence) {
				return true;
			}
		}
		return false;
	}

	// mention实体对之间的动词
	public void makeFeatures_VerbsInBetween(EntityPairIsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {
		boolean ifUseBOW = false;
		boolean ifUsePOS = true;
		boolean ifUseLemma = true;

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
			Mention latterMention = null;
			if (chemMention.compareTo(disMention) < 0) {
				formerMention = chemMention;
				latterMention = disMention;
			} else {
				formerMention = disMention;
				latterMention = chemMention;
			}

			List<Token> tokensInBetween = relationSource
					.getTokensBetweenTwoMentions(chemMention, disMention);

			List<Token> tokensAfterFilter = filterTokensInParenthesis(
					tokensInBetween);
			List<Token> verbTokensInBetween = new ArrayList<Token>();
			for (Token t : tokensAfterFilter) {
				String stanfordPOS = t.getStanfordPOS();
				if (stanfordPOS.indexOf("VB") != -1) {
					verbTokensInBetween.add(t);
				}
			}

			if (verbTokensInBetween.isEmpty()) {
				String noVerbInBetweenPrefix = "noVerbInBetween@@None";
				fvWords.add(noVerbInBetweenPrefix);
				fvValues.add(1.0);
			} else if (verbTokensInBetween.size() == 1) {
				Token t = verbTokensInBetween.get(0);
				String ifOnlyVerbInBetweenPrefix = "ifOnlyVerbWordInBetween@@True";
				fvWords.add(ifOnlyVerbInBetweenPrefix);
				fvValues.add(1.0);

				String ifExistOtherMentionBeforeOnlyVerb = "ifExistOtherMentionBeforeOnlyVerb@@True";
				if (hasAnyOtherMentionBeforeTheToken(formerMention, ab, t)) {
					fvWords.add(ifExistOtherMentionBeforeOnlyVerb);
					fvValues.add(1.0);
				}

				String ifExistOtherMentionAfterOnlyVerb = "ifExistOtherMentionAfterOnlyVerb@@True";
				if (hasAnyOtherMentionAfterTheToken(latterMention, ab, t)) {
					fvWords.add(ifExistOtherMentionAfterOnlyVerb);
					fvValues.add(1.0);
				}

				if (ifUseBOW) {
					String onlyVerbWordInBetweenPrefix = "onlyVerbWordInBetween@@";
					String word = t.getText();
					fvWords.add(onlyVerbWordInBetweenPrefix + word);
					fvValues.add(1.0);
				}
				if (ifUsePOS) {
					String onlyVerbPOSInBetweenPrefix = "onlyVerbPOSInBetween@@";
					String stanfordPOS = t.getStanfordPOS();
					fvWords.add(onlyVerbPOSInBetweenPrefix + stanfordPOS);
					fvValues.add(1.0);
				}
				if (ifUseLemma) {
					String onlyVerbLemmaInBetweenPrefix = "onlyVerbLemmaInBetween@@";
					String lemma = t.getLemma();
					fvWords.add(onlyVerbLemmaInBetweenPrefix + lemma);
					fvValues.add(1.0);
				}
			} else {
				Token fVerbToken = verbTokensInBetween.get(0);
				Token lVerbToken = verbTokensInBetween
						.get(verbTokensInBetween.size() - 1);
				String ifExistOtherMentionBeforeFirstVerb = "ifExistOtherMentionBeforeFirstVerb@@True";
				if (hasAnyOtherMentionBeforeTheToken(formerMention, ab,
						fVerbToken)) {
					fvWords.add(ifExistOtherMentionBeforeFirstVerb);
					fvValues.add(1.0);
				}

				String ifExistOtherMentionAfterLastVerb = "ifExistOtherMentionAfterLastVerb@@True";
				if (hasAnyOtherMentionAfterTheToken(latterMention, ab,
						lVerbToken)) {
					fvWords.add(ifExistOtherMentionAfterLastVerb);
					fvValues.add(1.0);
				}

				if (ifUseBOW) {
					Token firstVerbToken = verbTokensInBetween.get(0);
					String firstVerbWordInBetweenPrefix = "firstVerbWordInBetween@@";
					String fword = firstVerbToken.getText();
					fvWords.add(firstVerbWordInBetweenPrefix + fword);
					fvValues.add(1.0);

					Token lastVerbToken = verbTokensInBetween
							.get(verbTokensInBetween.size() - 1);
					String lastVerbWordInBetweenPrefix = "lastVerbWordInBetween@@";
					String lword = lastVerbToken.getText();
					fvWords.add(lastVerbWordInBetweenPrefix + lword);
					fvValues.add(1.0);

					String otherVerbWordInBetweenPrefix = "otherVerbWordInBetween@@";
					String tmp = "";
					for (int i = 1; i < verbTokensInBetween.size() - 1; i++) {
						Token tt = verbTokensInBetween.get(i);
						if (i < verbTokensInBetween.size() - 2)
							tmp = tmp + tt.getText() + "_";
						else
							tmp = tmp + tt.getText();
					}
					fvWords.add(otherVerbWordInBetweenPrefix + tmp);
					fvValues.add(1.0);
				}
				if (ifUsePOS) {
					Token t = verbTokensInBetween.get(0);
					String firstVerbPOSInBetweenPrefix = "firstVerbPOSInBetween@@";
					String stanfordPOS = t.getStanfordPOS();
					fvWords.add(firstVerbPOSInBetweenPrefix + stanfordPOS);
					fvValues.add(1.0);

					Token lastVerbToken = verbTokensInBetween
							.get(verbTokensInBetween.size() - 1);
					String lastVerbPOSInBetweenPrefix = "lastVerbPOSInBetween@@";
					String lStanfordPOS = lastVerbToken.getStanfordPOS();
					fvWords.add(lastVerbPOSInBetweenPrefix + lStanfordPOS);
					fvValues.add(1.0);

					String otherVerbPOSInBetweenPrefix = "otherVerbPOSInBetween@@";
					String tmp = "";
					for (int i = 1; i < verbTokensInBetween.size() - 1; i++) {
						Token tt = verbTokensInBetween.get(i);
						if (i < verbTokensInBetween.size() - 2)
							tmp = tmp + tt.getStanfordPOS() + "_";
						else
							tmp = tmp + tt.getStanfordPOS();
					}
					fvWords.add(otherVerbPOSInBetweenPrefix + tmp);
					fvValues.add(1.0);
				}
				if (ifUseLemma) {
					Token t = verbTokensInBetween.get(0);
					String firstVerbLemmaInBetweenPrefix = "firstVerbLemmaInBetween@@";
					String lemma = t.getLemma();
					fvWords.add(firstVerbLemmaInBetweenPrefix + lemma);
					fvValues.add(1.0);

					Token lastVerbToken = verbTokensInBetween
							.get(verbTokensInBetween.size() - 1);
					String lastVerbLemmaInBetweenPrefix = "lastVerbLemmaInBetween@@";
					String lLemma = lastVerbToken.getLemma();
					fvWords.add(lastVerbLemmaInBetweenPrefix + lLemma);
					fvValues.add(1.0);

					String otherVerbLemmaInBetweenPrefix = "otherVerbLemmaInBetween@@";
					String tmp = "";
					for (int i = 1; i < verbTokensInBetween.size() - 1; i++) {
						Token tt = verbTokensInBetween.get(i);
						if (i < verbTokensInBetween.size() - 2)
							tmp = tmp + tt.getLemma() + "_";
						else
							tmp = tmp + tt.getLemma();
					}
					fvWords.add(otherVerbLemmaInBetweenPrefix + tmp);
					fvValues.add(1.0);
				}
			}
		}
	}

	public void makeFeatures_WordsInBetween(EntityPairIsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseBOW, boolean ifUseStopWordsList, boolean ifUsePOS,
			boolean ifUseLemma, boolean ifFilterPunc) {
		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		for (MentionPairAsInstanceSource mp : menPairs) {
			Mention chemMention = mp.getChemMention();
			Mention disMention = mp.getDisMention();
			Relation relationSource = source.getRelationSource();
			List<Token> tokensInBetween = relationSource
					.getTokensBetweenTwoMentions(chemMention, disMention);
			List<Token> tokensAfterFilter = filterTokensInParenthesis(
					tokensInBetween);
			if (ifFilterPunc) {
				List<Token> tmp = new ArrayList<Token>();
				for (Token t : tokensAfterFilter) {
					String text = t.getText();
					if (!isPunctuationExcludingHyphen(text)) {
						tmp.add(t);
					}
				}
				tokensAfterFilter = tmp;
			}

			if (ifUseStopWordsList) {
				List<Token> tmp = new ArrayList<Token>();
				for (Token t : tokensAfterFilter) {
					if (stopWords.contains(t.getText().toLowerCase())) {
						continue;
					} else {
						tmp.add(t);
					}
				}
				tokensAfterFilter = tmp;
			}

			if (ifUseBOW) {
				if (tokensAfterFilter.size() == 1) {
					// the only word in between
					String onlyWordInBetweenPrefix = "onlyWordInBetween@@";
					Token t = tokensAfterFilter.get(0);
					fvWords.add(onlyWordInBetweenPrefix
							+ t.getText().toLowerCase());
					fvValues.add(1.0);
				} else if (tokensAfterFilter.size() > 1) {
					// firstWordInBetween
					String firstWordInBetweenPrefix = "firstWordInBetween@@";
					fvWords.add(firstWordInBetweenPrefix
							+ tokensAfterFilter.get(0).getText().toLowerCase());
					fvValues.add(1.0);

					// lastWordInBetween
					String lastWordInBetweenPrefix = "lastWordInBetween@@";
					fvWords.add(lastWordInBetweenPrefix + tokensAfterFilter
							.get(tokensAfterFilter.size() - 1).getText()
							.toLowerCase());
					fvValues.add(1.0);

					// otherWordsInBetween
					String otherWordsInBetweenPrefix = "otherWordsInBetween@@";
					String otherWords = "";
					for (int i = 1; i < tokensAfterFilter.size() - 1; i++) {
						Token t = tokensAfterFilter.get(i);
						otherWords += t.getText().toLowerCase();

						if (i < tokensAfterFilter.size() - 2) {
							otherWords += "_";
						}
					}
					if (!otherWords.isEmpty()) {
						fvWords.add(otherWordsInBetweenPrefix + otherWords);
						fvValues.add(1.0);
					}
				} else {
					String noWordsInBetweenPrefix = "noWordsInBetween@@NoWord";
					fvWords.add(noWordsInBetweenPrefix);
					fvValues.add(1.0);
				}

			}

			if (ifUsePOS) {
				if (tokensAfterFilter.size() == 1) {
					// the only word in between
					String onlyWordPOSInBetweenPrefix = "onlyWordPOSInBetween@@";
					Token t = tokensAfterFilter.get(0);
					fvWords.add(onlyWordPOSInBetweenPrefix
							+ t.getStanfordPOS().toLowerCase());
					// fvWords.add(onlyWordPOSInBetweenPrefix
					// + t.getPos().toLowerCase());
					fvValues.add(1.0);

				}
			} else if (tokensAfterFilter.size() > 1) {
				// part of speech
				String firstWordPOSInBetweenPrefix = "firstWordPOSInBetween@@";
				fvWords.add(firstWordPOSInBetweenPrefix
						+ tokensAfterFilter.get(0).getPos().toLowerCase());
				fvValues.add(1.0);

				String lastWordPOSInBetweenPrefix = "lastWordPOSInBetween@@";
				fvWords.add(lastWordPOSInBetweenPrefix
						+ tokensAfterFilter.get(tokensAfterFilter.size() - 1)
								.getPos().toLowerCase());
				fvValues.add(1.0);

				String otherWordsPOSs = "";
				String otherWordsPOSInBetweenPrefix = "otherWordsPOSInBetween@@";
				for (int i = 1; i < tokensAfterFilter.size() - 1; i++) {
					Token t = tokensAfterFilter.get(i);
					otherWordsPOSs += t.getText().toLowerCase();

					if (i < tokensAfterFilter.size() - 2) {
						otherWordsPOSs += "_";
					}
				}
				if (!otherWordsPOSs.isEmpty()) {
					fvWords.add(otherWordsPOSInBetweenPrefix + otherWordsPOSs);
					fvValues.add(1.0);
				}

			} else {
				String noWordsPOSInBetweenPrefix = "noWordsPOSInBetween@@NoPOS";
				fvWords.add(noWordsPOSInBetweenPrefix);
				fvValues.add(1.0);
			}

			if (ifUseLemma) {
				if (tokensAfterFilter.size() == 1) {
					// the only word in between
					String onlyWordLemmaInBetweenPrefix = "onlyWordLemmaInBetween@@";
					Token t = tokensAfterFilter.get(0);

					fvWords.add(onlyWordLemmaInBetweenPrefix
							+ t.getLemma().toLowerCase());
					fvValues.add(1.0);

				} else if (tokensAfterFilter.size() > 1) {
					String firstWordLemmaInBetween = "firstWordLemmaInBetween@@";
					fvWords.add(firstWordLemmaInBetween + tokensAfterFilter
							.get(0).getLemma().toLowerCase());
					fvValues.add(1.0);

					String lastWordLemmaInBetweenPrefix = "lastWordLemmaInBetween@@";
					fvWords.add(lastWordLemmaInBetweenPrefix + tokensAfterFilter
							.get(tokensAfterFilter.size() - 1).getLemma()
							.toLowerCase());
					fvValues.add(1.0);

					String otherWordsLemmaInBetweenPrefix = "otherWordsLemmaInBetween@@";
					String otherWordsLemmas = "";
					for (int i = 1; i < tokensAfterFilter.size() - 1; i++) {
						Token t = tokensAfterFilter.get(i);
						otherWordsLemmas += t.getText().toLowerCase();

						if (i < tokensAfterFilter.size() - 2) {
							otherWordsLemmas += "_";
						}
					}
					if (!otherWordsLemmas.isEmpty()) {
						fvWords.add(otherWordsLemmaInBetweenPrefix
								+ otherWordsLemmas);
						fvValues.add(1.0);
					}
				} else {
					// no word in between
					String noWordLemmaInBetween = "noWordLemmaInBetween@@NoLemma";
					fvWords.add(noWordLemmaInBetween);
					fvValues.add(1.0);
				}
			}
		}
	}

	public void makeFeatures_SyntacticPath_FromChemical2Disease(
			EntityPairIsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		boolean ifGeneralization = false;

		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		for (MentionPairAsInstanceSource mp : menPairs) {
			Mention chemMention = mp.getChemMention();
			Mention disMention = mp.getDisMention();
			Relation relationSource = source.getRelationSource();
			Abstract ab = relationSource.getAbstract();
			Sentence sentence = chemMention.getSentence(ab);

			String chem2DisSyntacticPathPrefix = "chem2DisSynPath@@";
			List<String> chem2DisSynPath = sentence
					.getSyntacticPathFromChemicalMention2DiseaseMention(
							chemMention, disMention);

			if (!chem2DisSynPath.isEmpty()) {
				String pathStr = "";
				if (ifGeneralization) {
					List<String> genericPath = new ArrayList<String>();

					for (int i = 0; i < chem2DisSynPath.size(); i++) {
						if (i == 0) {
							genericPath.add(chem2DisSynPath.get(i));
						} else {
							String lastTagInPath = genericPath
									.get(genericPath.size() - 1);
							String currentTagInPath = chem2DisSynPath.get(i);
							if (!lastTagInPath.equals(currentTagInPath)) {
								genericPath.add(currentTagInPath);
							}
						}
					}

					for (int i = 0; i < genericPath.size(); i++) {
						pathStr += genericPath.get(i);
						if (i < genericPath.size() - 1) {
							pathStr += "_";
						}
					}

				} else {
					for (int i = 0; i < chem2DisSynPath.size(); i++) {
						pathStr += chem2DisSynPath.get(i);
						if (i < chem2DisSynPath.size() - 1) {
							pathStr += "_";
						}
					}
				}

				fvWords.add(chem2DisSyntacticPathPrefix + pathStr);
				fvValues.add(1.0);
			}
		}
	}

	public void makeFeatures_SyntacticPath_FromDisease2Chemical(
			EntityPairIsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		boolean ifGeneralization = false;

		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		for (MentionPairAsInstanceSource mp : menPairs) {
			Mention chemMention = mp.getChemMention();
			Mention disMention = mp.getDisMention();
			Relation relationSource = source.getRelationSource();
			Abstract ab = relationSource.getAbstract();
			Sentence sentence = chemMention.getSentence(ab);

			String dis2ChemSyntacticPathPrefix = "dis2ChemSynPath@@";
			List<String> dis2ChemSynPath = sentence
					.getSyntacticPathFromDiseaseMention2ChemicalMention(
							disMention, chemMention);
			if (!dis2ChemSynPath.isEmpty()) {
				String pathStr = "";
				if (ifGeneralization) {
					List<String> genericPath = new ArrayList<String>();

					for (int i = 0; i < dis2ChemSynPath.size(); i++) {
						if (i == 0) {
							genericPath.add(dis2ChemSynPath.get(i));
						} else {
							String lastTagInPath = genericPath
									.get(genericPath.size() - 1);
							String currentTagInPath = dis2ChemSynPath.get(i);
							if (!lastTagInPath.equals(currentTagInPath)) {
								genericPath.add(currentTagInPath);
							}
						}
					}

					for (int i = 0; i < genericPath.size(); i++) {
						pathStr += genericPath.get(i);
						if (i < genericPath.size() - 1) {
							pathStr += "_";
						}
					}
				} else {
					for (int i = 0; i < dis2ChemSynPath.size(); i++) {
						pathStr += dis2ChemSynPath.get(i);
						if (i < dis2ChemSynPath.size() - 1) {
							pathStr += "_";
						}
					}
				}

				fvWords.add(dis2ChemSyntacticPathPrefix + pathStr);
				fvValues.add(1.0);
			}
		}
	}

	public void makeFeatures_DependencyPath(EntityPairIsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {

		Set<MentionPairAsInstanceSource> menPairs = source
				.getUsedMentionPairs();
		if (menPairs.isEmpty())
			return;

		for (MentionPairAsInstanceSource mp : menPairs) {
			Mention chemMention = mp.getChemMention();
			Mention disMention = mp.getDisMention();
			Relation relationSource = source.getRelationSource();
			Abstract ab = relationSource.getAbstract();
			Sentence sentence = chemMention.getSentence(ab);

			String chem2DisDependencyPathPrefix = "chem2DisDepPath@@";
			List<SemanticGraphEdge> chem2DisShortestDepPath = sentence
					.getShortestUndirectedPathEdges(chemMention, disMention);

			if (!chem2DisShortestDepPath.isEmpty()) {
				String pathStr = "";
				for (int i = 0; i < chem2DisShortestDepPath.size(); i++) {
					pathStr += chem2DisShortestDepPath.get(i).getRelation()
							.getShortName();
					if (i < chem2DisShortestDepPath.size() - 1) {
						pathStr += "_";
					}
				}

				fvWords.add(chem2DisDependencyPathPrefix + pathStr);
				fvValues.add(1.0);
			}

			String dis2ChemDependencyPathPrefix = "dis2ChemDepPath@@";
			List<SemanticGraphEdge> dis2ChemShortestDepPath = sentence
					.getShortestUndirectedPathEdges(disMention, chemMention);
			if (!dis2ChemShortestDepPath.isEmpty()) {
				String pathStr = "";
				for (int i = 0; i < dis2ChemShortestDepPath.size(); i++) {
					pathStr += dis2ChemShortestDepPath.get(i).getRelation()
							.getShortName();
					if (i < dis2ChemShortestDepPath.size() - 1) {
						pathStr += "_";
					}
				}

				fvWords.add(dis2ChemDependencyPathPrefix + pathStr);
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
				true);
		makeFeatures_LeftNGram(source, fvWords, fvValues, false, true, true,
				true);
		makeFeatures_RightNGram(source, fvWords, fvValues, false, true, true,
				true);
		makeFeatures_VerbsInBetween(source, fvWords, fvValues);

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
