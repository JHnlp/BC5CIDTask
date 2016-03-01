package gjh.bc5.features;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import gjh.bc5.main.BC5Runner;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Sentence;
import gjh.bc5.utils.Token;

/**
 * 
 * @author GJH
 * 
 */
public class Relation2FeatureVector_MentionLevel_Cooccur extends Pipe {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7662165001347965854L;

	private static final int NGram_Window_Size = 3;

	public Relation2FeatureVector_MentionLevel_Cooccur(Alphabet dataAlphbet,
			LabelAlphabet targetAlphabet) {
		super(dataAlphbet, targetAlphabet);
		File stopWordsFile = new File("english_stopwords.tsv");
		if (!stopWordsFile.exists())
			throw new IllegalStateException("Unknown stopwords file!");

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

	public void makeFeatures_MentionsBOW(MentionPairAsInstanceSource mpi,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseMentionBOW, boolean ifUseStopWordsList,
			boolean ifUsePOS, boolean ifUseLemma) {
		Relation relationSource = mpi.getRelationSource();
		Mention chemMention = mpi.getChemMention();
		Mention disMention = mpi.getDisMention();
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

	public void makeFeatures_LeftNGram(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseBOW, boolean ifUseStopWordsList, boolean ifUsePOS,
			boolean ifUseLemma) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();

		Set<String> stopWords = BC5Runner.getStopWords();

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
				fvWords.add(menLeftNGramWordPrefix + t.getText().toLowerCase());
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
				fvWords.add(
						menLeftNGramLemmaPrefix + t.getLemma().toLowerCase());
				fvValues.add(1.0);
			}
		}
	}

	public void makeFeatures_RightNGram(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseBOW, boolean ifUseStopWordsList, boolean ifUsePOS,
			boolean ifUseLemma) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Set<String> stopWords = BC5Runner.getStopWords();

		Mention formerMention = null;
		Mention latterMention = null;
		if (chemMention.compareTo(disMention) < 0) {
			formerMention = chemMention;
			latterMention = disMention;
		} else {
			formerMention = disMention;
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
				fvWords.add(
						menRightNGramWordPrefix + t.getText().toLowerCase());
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
				fvWords.add(
						menRightNGramLemmaPrefix + t.getLemma().toLowerCase());
				fvValues.add(1.0);
			}
		}
	}

	public void makeFeatures_MentionLiteralStructures(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();

		String chemical_text = chemMention.getConceptID();
		String disease_text = disMention.getConceptID();

		String chemicalTextPrefix = "ChemMentionText@@";
		fvWords.add(chemicalTextPrefix + chemical_text);
		fvValues.add(1.0);

		String diseaseTextPrefix = "DisMentionText@@";
		fvWords.add(diseaseTextPrefix + disease_text);
		fvValues.add(1.0);

		String whetherChemInSpecialStructure = "ChemInSpecialStructure@@True";

		String ab_text = ab.getWholeText();
		int chem_endOff_inDoc = chemMention.getEndOffsetInDocument();

		if (chem_endOff_inDoc < ab_text.length()) {
			String tmp = ab_text.substring(chem_endOff_inDoc);
			if (tmp.startsWith("- ") || tmp.startsWith("-and")) {
				fvWords.add(whetherChemInSpecialStructure);
				fvValues.add(1.0);
			}
		}

	}

	public void makeFeatures_VerbsInBetween(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {
		boolean ifUseBOW = true;
		boolean ifUsePOS = true;
		boolean ifUseLemma = true;

		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
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

	public void makeFeatures_NegationWordsInBetween(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();

		List<Token> tokensInBetween = relationSource
				.getTokensBetweenTwoMentions(chemMention, disMention);

		String negationWordsInBetweenPrefix = "negationWordsInBetween@@";
		for (int i = 0; i < tokensInBetween.size() - 1; i++) {
			Token t = tokensInBetween.get(i);
			String word = t.getText();
			if (word.equalsIgnoreCase("none") || word.equalsIgnoreCase("no")
					|| word.equalsIgnoreCase("not")
					|| word.equalsIgnoreCase("never")) {
				fvWords.add(negationWordsInBetweenPrefix + word);
				fvValues.add(1.0);
			}
		}
	}

	public void makeFeatures_WordsInBetween(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues,
			boolean ifUseBOW, boolean ifUseStopWordsList, boolean ifUsePOS,
			boolean ifUseLemma, boolean ifFilterPunc) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Set<String> stopWords = BC5Runner.getStopWords();

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
				fvWords.add(
						onlyWordInBetweenPrefix + t.getText().toLowerCase());
				fvValues.add(1.0);
			} else if (tokensAfterFilter.size() > 1) {
				// firstWordInBetween
				String firstWordInBetweenPrefix = "firstWordInBetween@@";
				fvWords.add(firstWordInBetweenPrefix
						+ tokensAfterFilter.get(0).getText().toLowerCase());
				fvValues.add(1.0);

				// lastWordInBetween
				String lastWordInBetweenPrefix = "lastWordInBetween@@";
				fvWords.add(lastWordInBetweenPrefix
						+ tokensAfterFilter.get(tokensAfterFilter.size() - 1)
								.getText().toLowerCase());
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
			fvWords.add(lastWordPOSInBetweenPrefix + tokensAfterFilter
					.get(tokensAfterFilter.size() - 1).getPos().toLowerCase());
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
				fvWords.add(firstWordLemmaInBetween
						+ tokensAfterFilter.get(0).getLemma().toLowerCase());
				fvValues.add(1.0);

				String lastWordLemmaInBetweenPrefix = "lastWordLemmaInBetween@@";
				fvWords.add(lastWordLemmaInBetweenPrefix
						+ tokensAfterFilter.get(tokensAfterFilter.size() - 1)
								.getLemma().toLowerCase());
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
					fvWords.add(
							otherWordsLemmaInBetweenPrefix + otherWordsLemmas);
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

	public void makeFeatures_CooccurInTitle(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {
		Mention chemMention = source.getChemMention();
		int sentIndex = chemMention.getSentenceIndex();

		if (sentIndex == 0) {
			String cooccurInTile = "cooccurInTile@@True";
			fvWords.add(cooccurInTile);
			fvValues.add(1.0);
		}
	}

	// whether in title
	private void addFeatureVector_WhetherInTitle(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		// Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		int sent_index = sentence.getSentenceIndex();
		if (sent_index == 0) {
			String relation_pair_is_in_title = "relationPairIsInTitle@@True";
			fvWords.add(relation_pair_is_in_title);
			fvValues.add(1.0);
		}
	}

	// whether chemical is parsing root
	private void addFeatureVector_WhetherChemicalIsParsingRoot(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		// Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		SemanticGraph graph = sentence.getSemanticGraphFromSyntacticParsing();
		IndexedWord rootWord = graph.getFirstRoot();
		IndexedWord chemIndexedWord = sentence
				.getMentionVertexInDepGraph(chemMention);
		if (rootWord.equals(chemIndexedWord)) {
			String chemIsDepRoot = "chemIsDepRoot@@isRoot";
			fvWords.add(chemIsDepRoot);
			fvValues.add(1.0);
		}
	}

	// dependency path from root to chemical
	private void addFeatureVector_DependencyPathFromRoot2Chemical(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		// Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		SemanticGraph graph = sentence.getSemanticGraphFromSyntacticParsing();
		IndexedWord rootWord = graph.getFirstRoot();
		IndexedWord chemIndexedWord = sentence
				.getMentionVertexInDepGraph(chemMention);

		List<SemanticGraphEdge> root_to_chemical_edges = graph
				.getShortestDirectedPathEdges(rootWord, chemIndexedWord);
		String root2ChemDepPathPrefix = "root2ChemDepPath@@";
		if (root_to_chemical_edges != null
				&& !root_to_chemical_edges.isEmpty()) {
			String pathStr = "";
			for (int i = 0; i < root_to_chemical_edges.size(); i++) {
				pathStr += root_to_chemical_edges.get(i).getRelation()
						.toString();
				if (i < root_to_chemical_edges.size() - 1) {
					pathStr += "_";
				}
			}

			fvWords.add(root2ChemDepPathPrefix + pathStr);
			fvValues.add(1.0);
		}
	}

	// whether disease is parsing root
	private void addFeatureVector_WhetherDiseaseIsParsingRoot(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		SemanticGraph graph = sentence.getSemanticGraphFromSyntacticParsing();
		IndexedWord rootWord = graph.getFirstRoot();
		IndexedWord disIndexedWord = sentence
				.getMentionVertexInDepGraph(disMention);
		if (rootWord.equals(disIndexedWord)) {
			String disIsDepRoot = "disIsDepRoot@@isRoot";
			fvWords.add(disIsDepRoot);
			fvValues.add(1.0);
		}
	}

	// dependency path from chemical to disease
	private void addFeatureVector_DependencyPathFromChemical2Disease(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		String chem2DisDependencyPathPrefix = "chem2DisDepPath@@";
		List<SemanticGraphEdge> chem2DisShortestDepPath = sentence
				.getShortestUndirectedPathEdges(chemMention, disMention);
		if (chem2DisShortestDepPath != null
				&& !chem2DisShortestDepPath.isEmpty()) {
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
	}

	private void addFeatureVector_GenericDependencyPathFromChemical2Disease(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		String chem2DisGenericDependencyPathPrefix = "chem2DisGenericDepPath@@";
		List<SemanticGraphEdge> chem2DisShortestDepPath = sentence
				.getShortestUndirectedPathEdges(chemMention, disMention);
		if (chem2DisShortestDepPath != null
				&& !chem2DisShortestDepPath.isEmpty()) {

			List<SemanticGraphEdge> generic_edges = new ArrayList<SemanticGraphEdge>();
			generic_edges.add(chem2DisShortestDepPath.get(0));
			for (int i = 1; i < chem2DisShortestDepPath.size(); i++) {
				// previous edge
				SemanticGraphEdge pre_edge = chem2DisShortestDepPath.get(i - 1);
				// current edge
				SemanticGraphEdge cur_edge = chem2DisShortestDepPath.get(i);
				if (pre_edge.getRelation().getShortName()
						.equals(cur_edge.getRelation().getShortName())) {
					continue;
				} else {
					generic_edges.add(cur_edge);
				}
			}

			String pathStr = "";
			for (int i = 0; i < generic_edges.size(); i++) {
				pathStr += generic_edges.get(i).getRelation().getShortName();
				if (i < generic_edges.size() - 1) {
					pathStr += "_";
				}
			}

			fvWords.add(chem2DisGenericDependencyPathPrefix + pathStr);
			fvValues.add(1.0);
		}
	}

	// dependency path from root to disease
	private void addFeatureVector_DependencyPathFromRoot2Disease(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		SemanticGraph graph = sentence.getSemanticGraphFromSyntacticParsing();
		IndexedWord rootWord = graph.getFirstRoot();
		IndexedWord disIndexedWord = sentence
				.getMentionVertexInDepGraph(disMention);
		String root2DisDepPathPrefix = "root2DisDepPath@@";
		List<SemanticGraphEdge> root_to_disease_edges = graph
				.getShortestDirectedPathEdges(rootWord, disIndexedWord);
		if (root_to_disease_edges != null && !root_to_disease_edges.isEmpty()) {
			String pathStr = "";
			for (int i = 0; i < root_to_disease_edges.size(); i++) {
				pathStr += root_to_disease_edges.get(i).getRelation()
						.toString();
				if (i < root_to_disease_edges.size() - 1) {
					pathStr += "_";
				}
			}

			fvWords.add(root2DisDepPathPrefix + pathStr);
			fvValues.add(1.0);
		}
	}

	// dependency nodes sequence from chemical to disease
	private void addFeatureVector_DependencyNodesSequenceFromChemical2Disease(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		// get map, concealed text mapping to corresponding mentions
		List<Mention> mentions = sentence.getMentions();
		Map<String, String> concealedText_2_menText_map = new HashMap<String, String>();
		for (Mention men : mentions) {
			String men_concealed_text = men.getMentionAnonymousText();
			String mention_text = men.getText();
			concealedText_2_menText_map.put(men_concealed_text, mention_text);
		}

		List<IndexedWord> chem_2_dis_shortest_nodes = sentence
				.getShortestUndirectedPathNodes(chemMention, disMention);
		String chem2DisDepNodesSequencePrefix = "chem2DisDepNodesSequence@@";
		if (chem_2_dis_shortest_nodes != null
				&& !chem_2_dis_shortest_nodes.isEmpty()) {
			String pathStr = "";
			for (int i = 1; i < chem_2_dis_shortest_nodes.size() - 1; i++) {
				String wordText = chem_2_dis_shortest_nodes.get(i).word();
				String wordTag = chem_2_dis_shortest_nodes.get(i).tag();
				String wordLemma = Morphology.lemmaStatic(wordText, wordTag,
						true);

				String word_style_used = wordText;

				if (concealedText_2_menText_map.containsKey(wordText)) {
					word_style_used = concealedText_2_menText_map.get(wordText);
				} else {
					word_style_used = wordLemma;
				}

				if (i == 1) {
					pathStr += word_style_used.toLowerCase();
				} else {
					pathStr += "_" + word_style_used.toLowerCase();
				}
			}
			fvWords.add(chem2DisDepNodesSequencePrefix + pathStr);
			fvValues.add(1.0);
		}
	}

	// whether chemical and disease connect directly in dependency
	private void addFeatureVector_WhetherDirectDependency(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		SemanticGraph graph = sentence.getSemanticGraphFromSyntacticParsing();
		IndexedWord chemIndexedWord = sentence
				.getMentionVertexInDepGraph(chemMention);
		IndexedWord disIndexedWord = sentence
				.getMentionVertexInDepGraph(disMention);

		List<IndexedWord> path_nodes = graph.getShortestUndirectedPathNodes(
				chemIndexedWord, disIndexedWord);
		String directDependencyPrefix = "directDependency@@directDep";
		if (path_nodes != null && path_nodes.size() <= 2) {
			fvWords.add(directDependencyPrefix);
			fvValues.add(1.0);
		}
	}

	// verbs(lemma) in the dependency path from chemical to disease
	private void addFeatureVector_VerbsInDepPathFromChemical2Disease(
			MentionPairAsInstanceSource source, final List<String> fvWords,
			final List<Double> fvValues) {
		Relation relationSource = source.getRelationSource();
		Mention chemMention = source.getChemMention();
		Mention disMention = source.getDisMention();
		Abstract ab = relationSource.getAbstract();
		Sentence sentence = chemMention.getSentence(ab);

		List<IndexedWord> chem_2_dis_shortest_nodes = sentence
				.getShortestUndirectedPathNodes(chemMention, disMention);
		String chem2DisDepNodesSequencePrefix = "chem2DisDepNodesSequence@@";
		if (chem_2_dis_shortest_nodes != null
				&& !chem_2_dis_shortest_nodes.isEmpty()) {
			String pathStr = "";
			for (int i = 1; i < chem_2_dis_shortest_nodes.size() - 1; i++) {
				String wordText = chem_2_dis_shortest_nodes.get(i).word();
				String wordTag = chem_2_dis_shortest_nodes.get(i).tag();
				String wordLemma = Morphology.lemmaStatic(wordText, wordTag,
						true);

				String word_style_used = wordLemma;
				if (wordTag.startsWith("VB")) {
					pathStr += word_style_used.toLowerCase() + "_";
				}
			}

			if (pathStr.endsWith("_"))
				pathStr = pathStr.substring(0, pathStr.length() - 1);

			fvWords.add(chem2DisDepNodesSequencePrefix + pathStr);
			fvValues.add(1.0);
		}
	}

	public void makeFeatures_DependencyPath(MentionPairAsInstanceSource source,
			final List<String> fvWords, final List<Double> fvValues) {
		// whether in title
		addFeatureVector_WhetherInTitle(source, fvWords, fvValues);

		// whether chemical is parsing root
		addFeatureVector_WhetherChemicalIsParsingRoot(source, fvWords,
				fvValues);

		// dependency path from root to chemical
		addFeatureVector_DependencyPathFromRoot2Chemical(source, fvWords,
				fvValues);

		// whether disease is parsing root
		addFeatureVector_WhetherDiseaseIsParsingRoot(source, fvWords, fvValues);

		// dependency path from root to disease
		addFeatureVector_DependencyPathFromRoot2Disease(source, fvWords,
				fvValues);

		// dependency path from chemical to disease
		addFeatureVector_DependencyPathFromChemical2Disease(source, fvWords,
				fvValues);

		// // dependency path from chemical to disease
		// addFeatureVector_DependencyPathFromDisease2Chemical(source, fvWords,
		// fvValues);// not work

		// dependency nodes sequence from chemical to disease
		addFeatureVector_DependencyNodesSequenceFromChemical2Disease(source,
				fvWords, fvValues);

		// whether chemical and disease connect directly in dependency
		addFeatureVector_WhetherDirectDependency(source, fvWords, fvValues);

		// // chemical to disease dependency path with nodes
		// addFeatureVector_DependencyPathFromChemical2DiseaseWithNodes(source,
		// fvWords, fvValues);// not work

		addFeatureVector_GenericDependencyPathFromChemical2Disease(source,
				fvWords, fvValues);

		// verbs(lemma) in the dependency path from chemical to disease
		addFeatureVector_VerbsInDepPathFromChemical2Disease(source, fvWords,
				fvValues);
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

		// featrue vector

		makeFeatures_MentionsBOW(source, fvWords, fvValues, true, false, true,
				true);// work
		makeFeatures_LeftNGram(source, fvWords, fvValues, true, false, false,
				true);// work
		makeFeatures_RightNGram(source, fvWords, fvValues, true, false, false,
				true);// work
		makeFeatures_VerbsInBetween(source, fvWords, fvValues);// work
		makeFeatures_WordsInBetween(source, fvWords, fvValues, true, false,
				true, true, true);// work
		// sort of
		makeFeatures_MentionLiteralStructures(source, fvWords, fvValues);
		makeFeatures_DependencyPath(source, fvWords, fvValues);// work

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
		// carrier.setSource(relationSource);
		carrier.setSource(source);

		return carrier;
	}

}
