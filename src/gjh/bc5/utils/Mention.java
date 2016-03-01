package gjh.bc5.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gjh.bc5.dataset.GJHPubtatorDataset;

public class Mention extends SerialCloneable implements Comparable<Mention> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2216204091243796781L;
	private String documentID;
	private int sentenceIndex;
	private int startOffInDocument;
	private int endOffInDocument;
	private String text;
	private EntityType entityType;
	private Double probability;
	private String conceptID;
	private String nickyName;// some mentions have multiple conceptIDs, and
								// probably have aliases

	public Mention() {
		documentID = "";
		sentenceIndex = -1;// default value
		startOffInDocument = -1; // default value
		endOffInDocument = -1; // default value
		text = "";
		entityType = new EntityType();
		probability = 0.0;
		conceptID = "";
		nickyName = "";
	}

	public Mention(Mention m) {
		documentID = m.getDocumentID();
		sentenceIndex = m.getSentenceIndex();
		startOffInDocument = m.getStartOffsetInDocument();
		endOffInDocument = m.getEndOffsetInDocument();
		text = m.getText();
		entityType = new EntityType(m.getEntityType());
		probability = m.getProbability();
		conceptID = m.getConceptID();
		nickyName = m.getNickyName();
	}

	public Mention(String documentID, int sentenceIndex, int startOff,
			int endOff, String text, EntityType entityType2) {
		this(documentID, sentenceIndex, startOff, endOff, text, entityType2,
				null, null, null);
	}

	public Mention(String documentID, int sentenceIndex, int startOff,
			int endOff, String text, EntityType entityType, Double probability,
			String conceptID, String nickyName) {

		if (documentID == null)
			throw new IllegalArgumentException("documentID  cannot be empty!");
		this.documentID = documentID;

		if (sentenceIndex < 0)
			throw new IllegalArgumentException(
					"sentenceIndex  cannot be less than 0!");
		this.sentenceIndex = sentenceIndex;

		if (startOff < 0)
			throw new IllegalArgumentException("Invalid start: " + startOff);
		this.startOffInDocument = startOff;

		if (endOff <= 0)
			throw new IllegalArgumentException("Invalid end: " + endOff);
		this.endOffInDocument = endOff;
		if (length() <= 0)
			throw new IllegalArgumentException("Illegal length - startOffset: "
					+ startOff + " endOffset: " + endOff);

		if (text == null)
			throw new IllegalArgumentException("Text cannot be null");
		this.text = text;

		if (entityType == null)
			throw new IllegalArgumentException("entityType cannot be null");
		this.entityType = entityType;

		setProbability(probability);

		if (conceptID == null)
			this.conceptID = "";
		else
			this.conceptID = conceptID;

		if (nickyName == null)
			this.nickyName = "";
		else
			this.nickyName = nickyName;
	}

	public boolean isReady() {
		if (documentID.isEmpty() || sentenceIndex == -1
				|| startOffInDocument < 0 || endOffInDocument < 0
				|| startOffInDocument > endOffInDocument || text.isEmpty()
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
	 * @return A {@link EntityType} indicating the type of entity being
	 *         mentioned
	 */
	public EntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	public String getConceptID() {
		return conceptID;
	}

	public void setConceptID(String conceptID) {
		this.conceptID = conceptID;
	}

	public int getStartOffsetInDocument() {
		return startOffInDocument;
	}

	public void setNickyName(String nickyName) {
		this.nickyName = nickyName;
	}

	public String getNickyName() {
		return nickyName;
	}

	public int getStartOffsetInSentence(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID does not match!");

		List<Sentence> sentences = ab.getSentences();
		return startOffInDocument
				- sentences.get(sentenceIndex).getSentStartOffset();
	}

	public int getStartOffsetInSentence(Sentence s) {
		if (!s.getDocumentID().equals(this.documentID)
				|| s.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException("sentence does not match!");

		return startOffInDocument - s.getSentStartOffset();
	}

	public int getEndOffsetInDocument() {
		return endOffInDocument;
	}

	public int getEndOffsetInSentence(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID does not match!");

		List<Sentence> sentences = ab.getSentences();
		return endOffInDocument
				- sentences.get(sentenceIndex).getSentStartOffset();
	}

	public int getEndOffsetInSentence(Sentence s) {
		if (!s.getDocumentID().equals(this.documentID)
				|| s.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException("sentence does not match!");

		return endOffInDocument - s.getSentStartOffset();
	}

	public String getText() {
		return text;
	}

	/**
	 * @return The {@link Sentence} containing this {@link Mention}
	 */
	public Sentence getSentence(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID does not match!");

		List<Sentence> sentences = ab.getSentences();
		return sentences.get(sentenceIndex);
	}

	public int length() {
		return endOffInDocument - startOffInDocument;
	}

	// public boolean contains(int tokenIndex) {
	// return tokenIndex >= start && tokenIndex < end;
	// }

	public List<Token> getTokens(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID does not match!");

		int startInSent = getStartOffsetInSentence(ab);
		int endInSent = getEndOffsetInSentence(ab);

		Sentence sentence = getSentence(ab);

		int startTokenIndexInSent = sentence
				.getMentionStartTokenIndex(startInSent);
		int lastTokenIndexInset = sentence.getMentionLastTokenIndex(endInSent);// inclusive

		List<Token> tokens = sentence.getTokens();

		// System.out.println(sentence);
		// System.out.println(sentence.getTokens());

		List<Token> mentionTokens = tokens.subList(startTokenIndexInSent,
				lastTokenIndexInset + 1);
		return Collections.unmodifiableList(mentionTokens);
	}

	public List<Token> getTokens(Sentence s) {
		if (!s.getDocumentID().equals(this.documentID)
				|| s.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException("sentence does not match!");

		int startInSent = getStartOffsetInSentence(s);
		int endInSent = getEndOffsetInSentence(s);

		Sentence sentence = s;

		int startTokenIndexInSent = sentence
				.getMentionStartTokenIndex(startInSent);
		int lastTokenIndexInset = sentence.getMentionLastTokenIndex(endInSent);// inclusive

		List<Token> tokens = sentence.getTokens();

		List<Token> mentionTokens = tokens.subList(startTokenIndexInSent,
				lastTokenIndexInset + 1);
		return Collections.unmodifiableList(mentionTokens);
	}

	// the first token index of the mention in the sentence
	public int getStartTokenIndexInSentence(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID does not match!");

		int startInSent = getStartOffsetInSentence(ab);
		Sentence sentence = getSentence(ab);
		int startTokenIndexInSent = sentence
				.getMentionStartTokenIndex(startInSent);
		return startTokenIndexInSent;
	}

	// the first token index of the mention in the sentence
	public int getStartTokenIndexInSentence(Sentence s) {
		if (!s.getDocumentID().equals(this.documentID)
				|| s.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException("sentence does not match!");

		int startInSent = getStartOffsetInSentence(s);
		int startTokenIndexInSent = s.getMentionStartTokenIndex(startInSent);
		return startTokenIndexInSent;
	}

	// the last token index of the mention in the sentence (inclusive, not
	// exclusive)
	public int getLastTokenIndexInSentence(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID does not match!");

		int endInSent = getEndOffsetInSentence(ab);
		Sentence sentence = getSentence(ab);
		int lastTokenIndexInset = sentence.getMentionLastTokenIndex(endInSent);// inclusive
		return lastTokenIndexInset;// inclusive
	}

	// the last token index of the mention in the sentence (inclusive, not
	// exclusive)
	public int getLastTokenIndexInSentence(Sentence s) {
		if (!s.getDocumentID().equals(this.documentID)
				|| s.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException("sentence does not match!");

		int endInSent = getEndOffsetInSentence(s);
		int lastTokenIndexInset = s.getMentionLastTokenIndex(endInSent);// inclusive
		return lastTokenIndexInset;// inclusive
	}

	// public int getStartChar() {
	// return sentence.getTokens().get(start).getStart(false);
	// }
	//
	// public int getEndChar() {
	// return sentence.getTokens().get(end - 1).getEnd(false);
	// }
	//
	// public int getStartChar(boolean ignoreWhitespace) {
	// return sentence.getTokens().get(start).getStart(ignoreWhitespace);
	// }
	//
	// public int getEndChar(boolean ignoreWhitespace) {
	// return sentence.getTokens().get(end - 1).getEnd(ignoreWhitespace);
	// }

	/**
	 * Determines whether this {@link Mention} overlaps the specified
	 * {@link Mention}
	 */
	public boolean overlaps(Mention mention2) {
		return this.documentID.equals(mention2.getDocumentID())
				&& this.sentenceIndex == mention2.sentenceIndex
				&& endOffInDocument > mention2.startOffInDocument
				&& startOffInDocument < mention2.endOffInDocument;
	}

	public Double getProbability() {
		return probability;
	}

	public void setProbability(Double probability) {
		if (probability != null) {
			if (probability <= 0.0)
				throw new IllegalArgumentException(
						"Probability must be greater than 0.0: " + probability);
			if (probability > 1.0) {
				// FIXME Fix rounding error
				if (probability < 1.000001)
					probability = 1.0;
				else
					throw new IllegalArgumentException(
							"Probability may not exceed 1.0: " + probability);
			}
		}

		this.probability = probability;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((conceptID == null) ? 0 : conceptID.hashCode());
		result = prime * result
				+ ((documentID == null) ? 0 : documentID.hashCode());
		result = prime * result + endOffInDocument;
		result = prime * result
				+ ((entityType == null) ? 0 : entityType.hashCode());
		result = prime * result
				+ ((nickyName == null) ? 0 : nickyName.hashCode());
		result = prime * result + sentenceIndex;
		result = prime * result + startOffInDocument;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "Mention [documentID=" + documentID + ", sentenceIndex="
				+ sentenceIndex + ", startOffsetInDocument="
				+ startOffInDocument + ", endOffsetInDocument="
				+ endOffInDocument + ", text=" + text + ", entityType="
				+ entityType + ", conceptID=" + conceptID + ", nickyName="
				+ nickyName + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mention other = (Mention) obj;
		if (conceptID == null) {
			if (other.conceptID != null)
				return false;
		} else if (!conceptID.equals(other.conceptID))
			return false;
		if (documentID == null) {
			if (other.documentID != null)
				return false;
		} else if (!documentID.equals(other.documentID))
			return false;
		if (endOffInDocument != other.endOffInDocument)
			return false;
		if (entityType == null) {
			if (other.entityType != null)
				return false;
		} else if (!entityType.equals(other.entityType))
			return false;
		if (nickyName == null) {
			if (other.nickyName != null)
				return false;
		} else if (!nickyName.equals(other.nickyName))
			return false;
		if (sentenceIndex != other.sentenceIndex)
			return false;
		if (startOffInDocument != other.startOffInDocument)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	public int compareTo(Mention mention2) {
		Integer compare = getDocumentID().compareTo(mention2.getDocumentID());
		if (compare != 0)
			return compare;

		compare = startOffInDocument - mention2.startOffInDocument;
		if (compare != 0)
			return compare;

		compare = endOffInDocument - mention2.endOffInDocument;
		if (compare != 0)
			return compare;
		compare = entityType.getTypeName()
				.compareTo(mention2.entityType.getTypeName());
		if (compare != 0)
			return compare;

		compare = conceptID.compareTo(mention2.conceptID);
		if (compare != 0)
			return compare;

		return this.nickyName.compareTo(mention2.nickyName);
	}

	// return the token distance between the two mentions
	public int getTokenDistanceBetweenMentions(Mention other, Abstract ab) {

		if (!this.documentID.equals(other.documentID)
				|| !this.documentID.equals(ab.getDocumentID())) {
			throw new IllegalArgumentException("DocumentID does not match! ");
		}

		int distance = 0;
		if (this.compareTo(other) <= 0) {// this mention first
			List<Sentence> sentences = ab.getSentences();

			if (this.sentenceIndex == other.sentenceIndex) {// whether in the
															// same sentence
				Sentence sameSentence = this.getSentence(ab);
				if (this.overlaps(other))
					distance = 0;
				else {
					int firstMentionEndTokenIndex = this
							.getLastTokenIndexInSentence(sameSentence);

					int secondMentionStartTokenIndex = other
							.getStartTokenIndexInSentence(sameSentence);

					distance = secondMentionStartTokenIndex
							- firstMentionEndTokenIndex;
				}

			} else { // when in different sentences
				Sentence firstSentence = this.getSentence(ab);
				int firstMentionEndTokenIndex = this
						.getLastTokenIndexInSentence(firstSentence);
				int firstDistance = firstSentence.getTokens().size()
						- firstMentionEndTokenIndex;// the token distance
													// from the last
													// token of the mention
													// to the end of the
													// sentence

				int betweenDistance = 0;// token distance crossing sentences
				for (int i = this.sentenceIndex
						+ 1; i < other.sentenceIndex; i++) {
					betweenDistance += sentences.get(i).getTokens().size();
				}

				Sentence lastSentence = other.getSentence(ab);
				int mentionStartTokenIndex = other
						.getStartTokenIndexInSentence(lastSentence);
				int secondDistance = mentionStartTokenIndex;// the distance from
															// the start of the
															// sentence to the
															// first token of
															// the mention

				distance = firstDistance + betweenDistance + secondDistance;
			}
		} else { // other mention first
			List<Sentence> sentences = ab.getSentences();

			if (this.sentenceIndex == other.sentenceIndex) {
				Sentence sameSentence = this.getSentence(ab);
				if (this.overlaps(other))
					distance = 0;
				else {
					int firstMentionEndTokenIndex = other
							.getLastTokenIndexInSentence(sameSentence);

					int secondMentionStartTokenIndex = this
							.getStartTokenIndexInSentence(sameSentence);

					distance = secondMentionStartTokenIndex
							- firstMentionEndTokenIndex;
				}
			} else {
				Sentence firstSentence = other.getSentence(ab);
				int firstMentionEndTokenIndex = other
						.getLastTokenIndexInSentence(firstSentence);
				int firstDistance = firstSentence.getTokens().size()
						- firstMentionEndTokenIndex;// the token distance from
													// the last token of the
													// mention to the end of
													// the
													// sentence

				int betweenDistance = 0;// token distance crossing sentences
				for (int i = other.sentenceIndex
						+ 1; i < this.sentenceIndex; i++) {
					betweenDistance += sentences.get(i).getTokens().size();
				}

				Sentence lastSentence = this.getSentence(ab);
				int mentionStartTokenIndex = this
						.getStartTokenIndexInSentence(lastSentence);
				int secondDistance = mentionStartTokenIndex;// the distance from
															// the start of the
															// sentence to the
															// first token of
															// the mention

				distance = firstDistance + betweenDistance + secondDistance;
			}
		}

		return distance;
	}

	// left n tokens, in adverse sequence
	public List<Token> getLeftNGramTokensOfMention(Abstract ab, int ngram) {
		if (!this.documentID.equals(ab.getDocumentID())) {
			throw new IllegalArgumentException("DocumentID does not match!");
		}
		List<Token> leftTokens = new ArrayList<Token>();// in adverse sequence

		List<Sentence> sentences = ab.getSentences();
		Sentence sentence = sentences.get(this.sentenceIndex);
		List<Token> tokens = sentence.getTokens();
		int mentionStartTokenIndex = this
				.getStartTokenIndexInSentence(sentence);
		// int mentionEndTokenIndex = this.getEndTokenIndexInSentence(sentence);

		int window = ngram;
		if (window < 0) {
			throw new IllegalArgumentException("window size cannot be minus!");
		}
		if (window > mentionStartTokenIndex) {
			window = mentionStartTokenIndex;
		}

		for (int i = mentionStartTokenIndex - 1; i >= mentionStartTokenIndex
				- window; i--) {
			leftTokens.add(tokens.get(i));
		}

		return Collections.unmodifiableList(leftTokens);
	}

	// all tokens in the left, in ordinal sequence
	public List<Token> getAllLeftTokensOfMention(Abstract ab) {
		if (!this.documentID.equals(ab.getDocumentID())) {
			throw new IllegalArgumentException("DocumentID does not match!");
		}
		List<Token> leftTokens = new ArrayList<Token>();
		List<Sentence> sentences = ab.getSentences();
		Sentence sentence = sentences.get(this.sentenceIndex);
		List<Token> tokens = sentence.getTokens();
		int mentionStartTokenIndex = this
				.getStartTokenIndexInSentence(sentence);

		for (int i = 0; i <= mentionStartTokenIndex - 1; i++) {
			leftTokens.add(tokens.get(i));
		}

		return Collections.unmodifiableList(leftTokens);
	}

	// all tokens in the left excluding punctuations, in ordinal sequence
	public List<Token> getAllLeftTokensOfMentionWithoutPunctuations(
			Abstract ab) {
		List<Token> allLeftTokens = getAllLeftTokensOfMention(ab);
		List<Token> allLeftTokensWithoutPunctuations = new ArrayList<Token>();

		for (Token t : allLeftTokens) {
			if (!GJHPubtatorDataset.isPunctuation(t.getText())) {
				allLeftTokensWithoutPunctuations.add(t);
			}
		}

		return Collections.unmodifiableList(allLeftTokensWithoutPunctuations);
	}

	// return n tokens in the left excluding punctuations, in adverse sequence
	public List<Token> getLeftNGramTokensOfMentionWithoutPuctuations(
			Abstract ab, int ngram) {
		if (!this.documentID.equals(ab.getDocumentID())) {
			throw new IllegalArgumentException("DocumentID does not match!");
		}
		List<Token> leftTokens = getAllLeftTokensOfMentionWithoutPunctuations(
				ab);
		int size = leftTokens.size();
		List<Token> leftNGramTokens_inverseSort = new ArrayList<Token>();// in
																			// adverse
																			// sequence

		int window = ngram;
		if (window < 0) {
			throw new IllegalArgumentException("window size cannot be minus!");
		}
		if (window > size) {
			window = size;
		}

		for (int i = size - 1; i > size - 1 - window; i--) {
			leftNGramTokens_inverseSort.add(leftTokens.get(i));
		}

		return Collections.unmodifiableList(leftNGramTokens_inverseSort);
	}

	// right n tokens, in ordinal sequence
	public List<Token> getRightNGramTokensOfMention(Abstract ab, int ngram) {
		if (!this.documentID.equals(ab.getDocumentID())) {
			throw new IllegalArgumentException("DocumentID does not match!");
		}
		List<Token> rightTokens = new ArrayList<Token>();// in ordinal sequence

		List<Sentence> sentences = ab.getSentences();
		Sentence sentence = sentences.get(this.sentenceIndex);
		List<Token> tokens = sentence.getTokens();
		// int mentionStartTokenIndex =
		// this.getStartTokenIndexInSentence(sentence);
		int mentionEndTokenIndex = this.getLastTokenIndexInSentence(sentence);

		int window = ngram;
		if (window < 0) {
			throw new IllegalArgumentException("windows size cannot be minus!");
		}
		if (window >= tokens.size() - mentionEndTokenIndex) {
			window = tokens.size() - 1 - mentionEndTokenIndex;
		}

		for (int i = 1; i <= window; i++) {
			rightTokens.add(tokens.get(mentionEndTokenIndex + i));
		}

		return Collections.unmodifiableList(rightTokens);
	}

	// return all tokens in the right, in ordinal sequence
	public List<Token> getAllRightTokensOfMention(Abstract ab) {
		if (!this.documentID.equals(ab.getDocumentID())) {
			throw new IllegalArgumentException("DocumentID does not match!");
		}
		List<Token> rightTokens = new ArrayList<Token>();

		List<Sentence> sentences = ab.getSentences();
		Sentence sentence = sentences.get(this.sentenceIndex);
		List<Token> tokens = sentence.getTokens();
		int mentionLastTokenIndex = this.getLastTokenIndexInSentence(sentence);

		for (int i = mentionLastTokenIndex + 1; i < tokens.size(); i++) {
			rightTokens.add(tokens.get(i));
		}

		return Collections.unmodifiableList(rightTokens);
	}

	// return all tokens in the right excluding punctuations, in ordinal
	// sequence
	public List<Token> getAllRightTokensOfMentionWithoutPunctuations(
			Abstract ab) {
		List<Token> allRightTokens = getAllRightTokensOfMention(ab);
		List<Token> allRightTokensWithoutPunctuations = new ArrayList<Token>();

		for (Token t : allRightTokens) {
			if (!GJHPubtatorDataset.isPunctuation(t.getText())) {
				allRightTokensWithoutPunctuations.add(t);
			}
		}

		return Collections.unmodifiableList(allRightTokensWithoutPunctuations);
	}

	// return n tokens in the right excluding punctuations, in ordinal sequence
	public List<Token> getRightNGramTokensOfMentionWithoutPuctuations(
			Abstract ab, int ngram) {
		if (!this.documentID.equals(ab.getDocumentID())) {
			throw new IllegalArgumentException("DocumentID does not match!");
		}
		List<Token> rightTokens = getAllRightTokensOfMentionWithoutPunctuations(
				ab);
		int size = rightTokens.size();
		List<Token> rightNGramTokens = new ArrayList<Token>();

		int window = ngram;
		if (window < 0) {
			throw new IllegalArgumentException("windows size cannot be minus!");
		}
		if (window > size) {
			window = size;
		}

		for (int i = 0; i < window; i++) {
			rightNGramTokens.add(rightTokens.get(i));
		}

		return Collections.unmodifiableList(rightNGramTokens);
	}

	@Deprecated
	// the headword of the mention
	public String getMentionHeadWord(Abstract ab) {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID does not match!");

		Sentence s = getSentence(ab);
		return getMentionHeadWord(s);
	}

	@Deprecated
	// the headword of the mention
	public String getMentionHeadWord(Sentence s) {
		if (!s.getDocumentID().equals(this.documentID)
				|| s.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException("sentence does not match!");

		List<Token> tokens = this.getTokens(s);
		for (int i = 0; i < tokens.size(); i++) {
			Token t = tokens.get(i);
			if (t.getPos().equalsIgnoreCase("POS_IN")) {

				if (i == 0) {// the token of NO.0 is prep?
					System.err.println(t);
					System.err.println(
							"Why the start token of this mention is prep?");
					// throw new IllegalArgumentException(
					// "Why the start token of this mention is prep?");
					return tokens.get(tokens.size() - 1).getText();
				} else {
					return tokens.get(i - 1).getText();
				}
			}
		}

		return tokens.get(tokens.size() - 1).getText();
	}

	// return the anonymous format of the mention
	public String getMentionAnonymousText() {
		String anonymousString = "Doc_" + this.getDocumentID() + "_"
				+ this.getStartOffsetInDocument() + "_"
				+ this.getEndOffsetInDocument() + "_"
				+ this.getEntityType().getTypeName();

		return anonymousString;
	}

}
