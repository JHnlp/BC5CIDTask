package gjh.bc5.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * @author GJH
 * 
 */

public class Coreference extends SerialCloneable
		implements Comparable<Coreference> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4193148202716500437L;
	// private Abstract ab = null;
	private String documentID;
	private String conceptID;
	private EntityType entityType;
	private Set<Mention> mentions;

	public Coreference() {
		documentID = "";
		conceptID = "";
		mentions = new TreeSet<Mention>();
		entityType = new EntityType();
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public void setEntiyType(EntityType type) {
		this.entityType = type;
	}

	public Coreference(String documentID, String conceptID,
			Set<Mention> mentions) {
		if (documentID == null)
			throw new IllegalArgumentException("documentID cannot be null!");
		this.documentID = documentID;

		if (conceptID == null)
			throw new IllegalArgumentException("ConceptID cannot be null!");
		this.conceptID = conceptID;

		if (mentions == null || mentions.isEmpty())
			throw new IllegalArgumentException(
					"Mentions in coreference cannot be null!");
		this.mentions = mentions;

		Iterator<Mention> itr = mentions.iterator();
		entityType = itr.next().getEntityType();
	}

	public Coreference(String documentID, String conceptID, Mention mention) {
		if (documentID == null)
			throw new IllegalArgumentException("documentID cannot be null!");
		this.documentID = documentID;

		if (conceptID == null)
			throw new IllegalArgumentException("ConceptID cannot be null!");
		this.conceptID = conceptID;

		if (mention == null)
			throw new IllegalArgumentException(
					"Mentions in coreference cannot be null!");
		this.mentions = new TreeSet<Mention>();
		mentions.add(mention);

		entityType = mention.getEntityType();
	}

	public boolean isCoreferenceReady() {
		if (documentID == null || documentID.isEmpty() || conceptID == null
				|| conceptID.isEmpty() || mentions == null
				|| mentions.isEmpty())
			return false;
		return true;
	}

	public String getConceptID() {
		return conceptID;
	}

	public void setConceptID(String conceptID) {
		this.conceptID = conceptID;
	}

	public Set<Mention> getMentions() {
		return Collections.unmodifiableSet(mentions);
	}

	public void setMentions(Set<Mention> mentions) {
		this.mentions = mentions;
	}

	public boolean addMention(Mention m) {
		if (m.getConceptID().equals(conceptID)
				&& m.getEntityType().equals(entityType)) {
			return mentions.add(m);
		}
		return false;
	}

	public boolean removeMention(Mention m) {
		return mentions.remove(m);
	}

	public boolean isMentionEntityTypeConsistWithEachOther() {
		if (this.conceptID.equals(String.valueOf(-1)))
			return true;

		Iterator<Mention> itr = mentions.iterator();
		if (itr.hasNext()) {
			String type = itr.next().getEntityType().getTypeName();

			while (itr.hasNext()) {
				String ty = itr.next().getEntityType().getTypeName();
				if (!type.equals(ty))
					return false;
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((conceptID == null) ? 0 : conceptID.hashCode());
		result = prime * result
				+ ((documentID == null) ? 0 : documentID.hashCode());
		result = prime * result
				+ ((mentions == null) ? 0 : mentions.hashCode());
		result = prime * result
				+ ((entityType == null) ? 0 : entityType.hashCode());
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
		Coreference other = (Coreference) obj;
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
		if (mentions == null) {
			if (other.mentions != null)
				return false;
		} else if (!mentions.equals(other.mentions))
			return false;
		if (entityType == null) {
			if (other.entityType != null)
				return false;
		} else if (!entityType.equals(other.entityType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Coreference [documentID=" + documentID + ", conceptID="
				+ conceptID + ", type=" + entityType + ", mentions=" + mentions
				+ "]";
	}

	@Override
	public int compareTo(Coreference o) {
		// String thisCompareString = this.conceptID + "-" + this.type;
		// String otherCompareString = o.conceptID + "-" + o.type;
		// return thisCompareString.compareTo(otherCompareString);

		int compare = this.documentID.compareTo(o.documentID);
		if (compare != 0)
			return compare;
		compare = this.conceptID.compareTo(o.conceptID);
		if (compare != 0)
			return compare;
		compare = this.entityType.getTypeName()
				.compareTo(o.entityType.getTypeName());
		return compare;
	}

}
