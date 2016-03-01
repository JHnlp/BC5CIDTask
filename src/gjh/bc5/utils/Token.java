package gjh.bc5.utils;

import java.util.List;

public class Token extends SerialCloneable implements Comparable<Token> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3738261798998772316L;
	private String documentID;
	private int sentenceIndex;
	// private Sentence sentence;
	private int startOffInSentence;
	private int endOffInSentence;
	private String text;
	private String pos;
	private String lemma;
	private String stanfordPOS;

	public Token() {
		this.documentID = "";
		this.sentenceIndex = -1; // default value = -1
		this.startOffInSentence = -1; // default value = -1
		this.endOffInSentence = -1; // default value = -1
		this.text = "";
		this.pos = "";
		this.lemma = "";
		this.stanfordPOS = "";
	}

	// Copy Constructor, deep copy
	public Token(Token token) {
		super();
		this.documentID = token.getDocumentID();
		this.sentenceIndex = token.getSentenceIndex();
		this.startOffInSentence = token.getStartOffInSentence();
		this.endOffInSentence = token.getEndOffInSentence();
		this.text = token.getText();
		this.pos = token.getPos();
		this.lemma = token.getLemma();
		this.stanfordPOS = token.stanfordPOS;
	}

	public Token(String documentID, int sentenceIndex, int start, int end,
			String text, String pos, String lemma, String stanfordPOS) {
		if (documentID == null)
			throw new IllegalArgumentException("documentID  cannot be empty!");
		this.documentID = documentID;
		if (sentenceIndex < 0)
			throw new IllegalArgumentException(
					"sentenceIndex  cannot be less than 0!");
		this.sentenceIndex = sentenceIndex;

		if (start < 0)
			throw new IllegalArgumentException(
					"Start may not be less than 0: " + start);
		this.startOffInSentence = start;
		this.endOffInSentence = end;
		if (length() < 1)
			throw new IllegalArgumentException(
					"End must be greater than start; start: " + start + " end: "
							+ end);

		if (length() != text.length())
			throw new IllegalArgumentException(
					"Length dose not match the Text, length(): " + length()
							+ ", text: " + text);

		this.startOffInSentence = start;
		this.endOffInSentence = end;
		this.text = text;
		if (pos == null)
			this.pos = "";
		else
			this.pos = pos;
		if (lemma == null)
			this.lemma = "";
		else
			this.lemma = lemma;
		if (stanfordPOS == null)
			this.stanfordPOS = "";
		else
			this.stanfordPOS = stanfordPOS;
	}

	public Token(String documentID, int sentenceIndex, int start, int end,
			String text, String pos, String lemma) {
		this(documentID, sentenceIndex, start, end, text, pos, lemma, null);
	}

	public Token(String documentID, int sentenceIndex, int start, int end,
			String text) {
		this(documentID, sentenceIndex, start, end, text, null, null);
	}

	public boolean isReady() {
		if (documentID.isEmpty() || sentenceIndex == -1
				|| startOffInSentence < 0 || endOffInSentence < 0
				|| startOffInSentence > endOffInSentence || text.isEmpty()
				|| length() != text.length()) {
			return false;
		}
		return true;
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public int getSentenceIndex() {
		return sentenceIndex;
	}

	public void setSentenceIndex(int sentenceIndex) {
		this.sentenceIndex = sentenceIndex;
	}

	/**
	 * @return The start index for this token, inclusive
	 */
	public int getStartOffInSentence() {
		return startOffInSentence;
	}

	/**
	 * @return The end index for this token, exclusive
	 */
	public int getEndOffInSentence() {
		return endOffInSentence;
	}

	// /**
	// * @return The start index for this token, inclusive
	// */
	// public int getStart(boolean ignoreWhitespace) {
	// int value = start;
	// if (ignoreWhitespace)
	// value -= sentence.countWhitespace(start);
	// return value;
	// }
	//
	// /**
	// * @return The end index for this token, exclusive
	// */
	// public int getEnd(boolean ignoreWhitespace) {
	// int value = end;
	// if (ignoreWhitespace)
	// value -= sentence.countWhitespace(end) - 1;
	// return value;
	// }

	// whether contains the offset
	public boolean contains(int charOffsetInSentence) {
		return charOffsetInSentence >= startOffInSentence
				&& charOffsetInSentence < endOffInSentence;
	}

	/**
	 * @return The number of characters in this token
	 */
	public int length() {
		return endOffInSentence - startOffInSentence;
	}

	//
	// public Sentence getSentence() {
	// return sentence;
	// }
	//
	// public void setSentence(Sentence sentence) {
	// this.sentence = sentence;
	// }

	public void setStartOffInSentence(int start) {
		this.startOffInSentence = start;
	}

	public void setEndOffInSentence(int end) {
		this.endOffInSentence = end;
	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getStanfordPOS() {
		return stanfordPOS;
	}

	public void setStanfordPOS(String stanfordPOS) {
		this.stanfordPOS = stanfordPOS;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((documentID == null) ? 0 : documentID.hashCode());
		result = prime * result + endOffInSentence;
		result = prime * result + sentenceIndex;
		result = prime * result + startOffInSentence;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Token other = (Token) obj;
		if (documentID == null) {
			if (other.documentID != null)
				return false;
		} else if (!documentID.equals(other.documentID))
			return false;
		if (sentenceIndex != other.sentenceIndex)
			return false;
		if (startOffInSentence != other.startOffInSentence)
			return false;
		if (endOffInSentence != other.endOffInSentence)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Token [documentID=" + documentID + ", sentenceIndex="
				+ sentenceIndex + ", startOffInSentence=" + startOffInSentence
				+ ", endOffInSentence=" + endOffInSentence + ", text=" + text
				+ ", pos=" + pos + ", lemma=" + lemma + ", stanfordPOS="
				+ stanfordPOS + "]";
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int compareTo(Token token2) {
		// if (getDocumentID().equals(token2.getDocumentID())
		// && getSentenceIndex() == token2.getSentenceIndex()) {
		// Integer compare = token2.getStartOffInSentence()
		// - getStartOffInSentence();
		// if (compare != 0)
		// return compare;
		// return token2.getEndOffInSentence() - getEndOffInSentence();
		// } else {
		// throw new IllegalArgumentException(
		// "token1 and token2 should be in the same sentence!");
		// }

		Integer compare = getDocumentID().compareTo(token2.getDocumentID());
		if (compare != 0)
			return compare;

		compare = getSentenceIndex() - token2.getSentenceIndex();
		if (compare != 0)
			return compare;

		compare = getStartOffInSentence() - token2.getStartOffInSentence();
		if (compare != 0)
			return compare;

		return getEndOffInSentence() - token2.getEndOffInSentence();
	}

	public Sentence getSentence(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID  does not match!");

		List<Sentence> sentences = ab.getSentences();
		return sentences.get(sentenceIndex);
	}

	public int getTokenIndexInSentence(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID  does not match!");

		Sentence sentence = ab.getSentences().get(this.sentenceIndex);
		List<Token> tokens = sentence.getTokens();
		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.get(i).equals(this)) {
				return i;
			}
		}

		return -1;
	}

	public int getTokenIndexInSentence(Sentence sentence) {
		if (!sentence.getDocumentID().equals(this.documentID)
				|| sentence.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException("Sentence does not match!");

		List<Token> tokens = sentence.getTokens();
		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.get(i).equals(this)) {
				return i;
			}
		}

		return -1;
	}

}
