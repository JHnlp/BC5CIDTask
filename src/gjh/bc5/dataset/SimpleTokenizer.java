package gjh.bc5.dataset;

import java.util.ArrayList;
import java.util.List;

import gjh.bc5.utils.Sentence;
import gjh.bc5.utils.Token;

/**
 * Tokens ouput by this tokenizer consist of a contiguous block of alphanumeric
 * characters or a single punctuation mark. Note, therefore, that any
 * construction which contains a punctuation mark (such as a contraction or a
 * real number) will necessarily span over at least three tokens.
 * 
 */
public class SimpleTokenizer implements Tokenizer {

	private static boolean isPunctuation(char ch) {
		return ("`~!@#$%^&*()-–=_+[]\\{}|;':\",./<>?".indexOf(ch) != -1);
	}

	public SimpleTokenizer() {
		// Empty
	}

	public void tokenize(Sentence sentence) {
		String text = sentence.getText();
		sentence.clearTokens();
		int start = 0;
		for (int i = 1; i - 1 < text.length(); i++) {
			char current = text.charAt(i - 1);
			char next = 0;
			if (i < text.length())
				next = text.charAt(i);
			if (Character.isSpaceChar(current)) {
				start = i;
			} else if (Character.isLetter(current)
					|| Character.isDigit(current)) {
				if (!Character.isLetter(next) && !Character.isDigit(next)) {
					sentence.addToken(new Token(sentence.getDocumentID(),
							sentence.getSentenceIndex(), start, i,
							sentence.getText(start, i)));
					start = i;
				}
			} else if (isPunctuation(current)) {
				sentence.addToken(new Token(sentence.getDocumentID(),
						sentence.getSentenceIndex(), start, i,
						sentence.getText(start, i)));
				start = i;
			}
		}
		if (start < text.length())
			sentence.addToken(new Token(sentence.getDocumentID(),
					sentence.getSentenceIndex(), start, text.length(),
					sentence.getText(start, text.length())));
	}

	public List<String> getTokens(String text) {
		int start = 0;
		List<String> token_texts = new ArrayList<String>();
		for (int i = 1; i - 1 < text.length(); i++) {
			char current = text.charAt(i - 1);
			char next = 0;
			if (i < text.length())
				next = text.charAt(i);
			if (Character.isSpaceChar(current)) {
				start = i;
			} else if (Character.isLetter(current)
					|| Character.isDigit(current)) {
				if (!Character.isLetter(next) && !Character.isDigit(next)) {
					token_texts.add(text.substring(start, i));
					start = i;
				}
			} else if (isPunctuation(current)) {
				token_texts.add(text.substring(start, i));
				start = i;
			}
		}
		if (start < text.length())
			token_texts.add(text.substring(start, text.length()));
		return token_texts;
	}

	public static void main(String[] args) {
		Sentence s = new Sentence("26094", 0,
				"Antihypertensive drugs and depression: a reappraisal.", 0);
		SimpleTokenizer nizer = new SimpleTokenizer();
		nizer.tokenize(s);
		Sentence s2 = (Sentence) s.clone();
		nizer.tokenize(s2);

		System.out.println(s.getTokens());
		System.out.println(s2.getTokens());

		List<Token> t1 = s.getTokens();
		List<Token> t2 = s2.getTokens();
		for (int i = 0; i < t1.size(); i++)
			System.out.println(t1.get(i) == t2.get(i));

	}
}
