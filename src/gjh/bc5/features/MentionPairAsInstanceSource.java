package gjh.bc5.features;

import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Sentence;

/**
 * 
 * @author GJH
 * 
 */

public class MentionPairAsInstanceSource
		implements Comparable<MentionPairAsInstanceSource> {

	private Mention chemMention;
	private Mention disMention;
	private Relation relation_source;

	public MentionPairAsInstanceSource(Mention chemMention, Mention disMention,
			Relation relationSource) {
		if (!chemMention.getDocumentID().equals(disMention.getDocumentID()))
			throw new IllegalArgumentException(
					"Chemical mention documentID does not match!");
		// if (chemMention.getSentenceIndex() != disMention.getSentenceIndex())
		// throw new IllegalArgumentException(
		// "Chemical mention sentence index does not match!");
		if (!chemMention.getDocumentID().equals(relationSource.getDocumentID()))
			throw new IllegalArgumentException(
					"Relation documentID does not match!");
		if (!chemMention.getConceptID()
				.equals(relationSource.getChemicalConceptID()))
			throw new IllegalArgumentException(
					"Chemical conceptID does not match!");
		if (!disMention.getConceptID()
				.equals(relationSource.getDiseaseConceptID()))
			throw new IllegalArgumentException(
					"Disease conceptID does not match!");

		this.chemMention = chemMention;
		this.disMention = disMention;
		this.relation_source = relationSource;
	}

	public Mention getChemMention() {
		return chemMention;
	}

	public Mention getDisMention() {
		return disMention;
	}

	public Relation getRelationSource() {
		return relation_source;
	}

	public Abstract getAbstract() {
		return relation_source.getAbstract();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((chemMention == null) ? 0 : chemMention.hashCode());
		result = prime * result
				+ ((disMention == null) ? 0 : disMention.hashCode());
		result = prime * result
				+ ((relation_source == null) ? 0 : relation_source.hashCode());
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
		MentionPairAsInstanceSource other = (MentionPairAsInstanceSource) obj;
		if (chemMention == null) {
			if (other.chemMention != null)
				return false;
		} else if (!chemMention.equals(other.chemMention))
			return false;
		if (disMention == null) {
			if (other.disMention != null)
				return false;
		} else if (!disMention.equals(other.disMention))
			return false;
		if (relation_source == null) {
			if (other.relation_source != null)
				return false;
		} else if (!relation_source.equals(other.relation_source))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MentionPairInstanceSource [chemMention=" + chemMention
				+ ", disMention=" + disMention + ", relationSource="
				+ relation_source + "]";
	}

	@Override
	public int compareTo(MentionPairAsInstanceSource o) {
		int compare = this.chemMention.compareTo(o.chemMention);
		if (compare != 0)
			return compare;

		compare = this.disMention.compareTo(o.disMention);
		if (compare != 0)
			return compare;

		compare = this.relation_source.compareTo(o.relation_source);

		return compare;
	}

	public int getTokenDistanceBetweenMentions() {
		int distance = relation_source
				.getTokensBetweenTwoMentions(chemMention, disMention).size();

		return distance;
	}

	public int getSentenceIndexOfCooccurrence() {
		return chemMention.getSentenceIndex();
	}

	public Sentence getSentenceOfCooccurrence() {
		Abstract a = this.getAbstract();
		Sentence s = this.chemMention.getSentence(a);
		return s;
	}

}
