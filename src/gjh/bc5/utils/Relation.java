package gjh.bc5.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * @author GJH
 * 
 */
public class Relation extends SerialCloneable implements Comparable<Relation> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -981297808468442600L;
	private String documentID;
	private Abstract ab;
	private String relationType;
	private String chemicalConceptID;
	private String diseaseConceptID;
	private Double probability;

	public Relation(String documentID, Abstract ab, String relationType,
			String chemicalConceptID, String diseaseConceptID) {
		this(documentID, ab, relationType, chemicalConceptID, diseaseConceptID,
				null);
	}

	public Relation(String documentID, Abstract ab, String relationType,
			String chemicalConceptID, String diseaseConceptID,
			Double probability) {
		if (documentID == null)
			throw new IllegalArgumentException("documentID cannot be null");
		this.documentID = documentID;

		// if (ab == null || !documentID.equals(ab.getDocumentID()))
		// throw new IllegalArgumentException("abstract cannot be null");
		this.ab = ab;

		if (relationType == null)
			throw new IllegalArgumentException("relationType cannot be null");
		this.relationType = relationType;

		if (chemicalConceptID == null)
			throw new IllegalArgumentException(
					"chemicalConceptID cannot be null");
		this.chemicalConceptID = chemicalConceptID;

		if (diseaseConceptID == null)
			throw new IllegalArgumentException(
					"diseaseConceptID cannot be null");
		this.diseaseConceptID = diseaseConceptID;

		if (probability == null || probability.compareTo(0.0) < 0)
			probability = 0.0;
		if (probability.compareTo(1.0) > 0)
			probability = 1.0;
		this.probability = probability;
	}

	// public Relation(Relation r){
	// documentID = r.getDocumentID();
	// ab = r.getAbstract();
	// relationType = r.getRelationType();
	// chemicalConceptID = r.getChemicalConceptID();
	// diseaseConceptID = r.getDiseaseConceptID();
	// }

	// public Relation clone() throws CloneNotSupportedException{
	// Relation cloned = (Relation)super.clone();
	// return cloned;
	// }

	public Abstract getAbstract() {
		return ab;
	}

	public void setAbstract(Abstract ab) {
		this.ab = ab;
	}

	public Double getProbability() {
		return probability;
	}

	public void setProbability(Double probability) {
		this.probability = probability;
	}

	public Coreference getChemicalCoreference() {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID  does not match!");

		Map<String, Coreference> map = ab.getChemicalCoreferences();
		return map.get(chemicalConceptID);
	}

	public Coreference getDiseaseCoreference() {
		if (!ab.getDocumentID().equals(this.documentID))
			throw new IllegalArgumentException("documentID  does not match!");

		Map<String, Coreference> map = ab.getDiseaseCoreferences();
		return map.get(diseaseConceptID);
	}

	// whether chemical mention and disease mention have cooccurrence
	public boolean hasCooccurrenceInOneSentence() {
		Coreference chemCoref = getChemicalCoreference();
		Coreference disCoref = getDiseaseCoreference();

		if (chemCoref == null || disCoref == null)
			return false;

		Set<Mention> chemicalMentions = chemCoref.getMentions();
		Set<Mention> diseaseMentions = disCoref.getMentions();

		if (chemicalMentions == null || diseaseMentions == null)
			return false;

		for (Mention c : chemicalMentions) {
			for (Mention d : diseaseMentions) {
				if (!c.getDocumentID().equals(d.getDocumentID()))
					throw new IllegalArgumentException(
							"documentID  does not match!");

				if (c.getSentenceIndex() == d.getSentenceIndex())
					return true;
			}
		}
		return false;
	}

	// reserve the corresponding mentions of chemical and disease
	public void getCoocurrenceMentionPairsInOneSentence(
			List<Mention> chemMentions, List<Mention> disMentions) {
		if (chemMentions == null || disMentions == null)
			throw new IllegalArgumentException("parameters are error!");

		chemMentions.clear();
		disMentions.clear();

		Coreference chemCoref = getChemicalCoreference();
		Coreference disCoref = getDiseaseCoreference();

		if (chemCoref == null || disCoref == null)
			return;

		Set<Mention> chemicalMentions = chemCoref.getMentions();
		Set<Mention> diseaseMentions = disCoref.getMentions();

		if (chemicalMentions == null || diseaseMentions == null)
			return;

		for (Mention c : chemicalMentions) {
			for (Mention d : diseaseMentions) {
				if (!c.getDocumentID().equals(d.getDocumentID()))
					throw new IllegalArgumentException(
							"documentID  does not match!");

				if (c.getSentenceIndex() == d.getSentenceIndex()) {
					chemMentions.add(c);
					disMentions.add(d);
				}
			}
		}
	}

	// get sentences that have cooccurrence
	public List<Sentence> getSentencesOfCooccurrence() {
		List<Mention> chemMentions = new ArrayList<Mention>();
		List<Mention> disMentions = new ArrayList<Mention>();
		Set<Sentence> sentencesOfCooccur = new TreeSet<Sentence>();
		getCoocurrenceMentionPairsInOneSentence(chemMentions, disMentions);

		for (Mention c : chemMentions) {
			sentencesOfCooccur.add(c.getSentence(ab));
		}

		return Collections
				.unmodifiableList(new ArrayList<Sentence>(sentencesOfCooccur));
	}

	public List<Mention> getTheNearestMentions() {
		List<Mention> list = new ArrayList<Mention>(2);
		Mention nearestChemMention = null;
		Mention nearestDisMention = null;

		Coreference chemicalCoref = getChemicalCoreference();
		Set<Mention> chemMentions = chemicalCoref.getMentions();
		Coreference diseaseCoref = getDiseaseCoreference();
		Set<Mention> disMentions = diseaseCoref.getMentions();

		Mention firstChemMen = chemMentions.iterator().next();
		Mention firstDisMen = disMentions.iterator().next();

		int nearestDistance = firstChemMen
				.getTokenDistanceBetweenMentions(firstDisMen, ab);
		nearestChemMention = firstChemMen;
		nearestDisMention = firstDisMen;

		Iterator<Mention> itrChem = chemMentions.iterator();
		while (itrChem.hasNext()) {
			Mention chemmen = itrChem.next();
			Iterator<Mention> itrDis = disMentions.iterator();
			while (itrDis.hasNext()) {
				Mention dismen = itrDis.next();

				int dis = chemmen.getTokenDistanceBetweenMentions(dismen, ab);

				if (dis == nearestDistance
						&& (!nearestChemMention.equals(chemmen)
								|| !nearestDisMention.equals(dismen))) {// if
																		// multiple?

					// do nothing

					// System.out.println("Current chemical mention is: "
					// + nearestChemMention);
					// System.out.println("Current disease mention is: "
					// + nearestDisMention);
					// System.out.println("New chemical mention is: " +
					// chemmen);
					// System.out.println("New disease mention is: " + dismen);
				}

				if (dis < nearestDistance) {
					nearestDistance = dis;
					nearestChemMention = chemmen;
					nearestDisMention = dismen;
				}
			}
		}

		// System.out
		// .println("Token distance between nearest chemical and disease
		// mentions is: "
		// + nearestDistance);

		list.add(nearestChemMention);
		list.add(nearestDisMention);
		return Collections.unmodifiableList(list);
	}

	public List<Token> getTokensBetweenTwoMentions(Mention mention1,
			Mention mention2) {
		if (!mention1.getDocumentID().equals(this.documentID)
				|| !mention2.getDocumentID().equals(this.documentID)) {
			throw new IllegalArgumentException("documentIDs don't match!");
		}

		if ((!mention1.getConceptID().equals(this.chemicalConceptID)
				&& !mention1.getConceptID().equals(this.diseaseConceptID))
				|| (!mention2.getConceptID().equals(this.chemicalConceptID)
						&& !mention2.getConceptID()
								.equals(this.diseaseConceptID))) {
			throw new IllegalArgumentException(
					"Mention ConceptID does not match!");
		}
		if (mention1.overlaps(mention2)) {
			System.err.println("Mentions are overlapping!");
			return new ArrayList<Token>();
		}

		List<Token> tokens = new ArrayList<Token>();

		if (mention1.compareTo(mention2) <= 0) {
			List<Sentence> sentences = ab.getSentences();

			if (mention1.getSentenceIndex() == mention2.getSentenceIndex()) {
				Sentence sameSentence = mention1.getSentence(ab);
				if (mention1.overlaps(mention2)) {
					// do nothing
				} else {
					int firstMentionEndTokenIndex = mention1
							.getLastTokenIndexInSentence(sameSentence);

					int secondMentionStartTokenIndex = mention2
							.getStartTokenIndexInSentence(sameSentence);

					List<Token> sentTokens = sameSentence.getTokens();
					for (int i = firstMentionEndTokenIndex
							+ 1; i < secondMentionStartTokenIndex; i++)
						tokens.add(sentTokens.get(i));
				}
			} else {
				Sentence firstSentence = mention1.getSentence(ab);
				int firstMentionEndTokenIndex = mention1
						.getLastTokenIndexInSentence(firstSentence);
				List<Token> firstSentTokens = firstSentence.getTokens();
				for (int i = firstMentionEndTokenIndex + 1; i < firstSentTokens
						.size(); i++)
					tokens.add(firstSentTokens.get(i));

				for (int i = mention1.getSentenceIndex() + 1; i < mention2
						.getSentenceIndex(); i++) {
					List<Token> middleSentTokens = sentences.get(i).getTokens();
					tokens.addAll(middleSentTokens);
				}

				Sentence lastSentence = mention2.getSentence(ab);
				int mentionStartTokenIndex = mention2
						.getStartTokenIndexInSentence(lastSentence);
				List<Token> lastSentTokens = lastSentence.getTokens();
				for (int i = 0; i < mentionStartTokenIndex; i++)
					tokens.add(lastSentTokens.get(i));
			}
		} else {
			List<Sentence> sentences = ab.getSentences();

			if (mention2.getSentenceIndex() == mention1.getSentenceIndex()) {
				Sentence sameSentence = mention2.getSentence(ab);
				if (mention2.overlaps(mention1)) {

				} else {
					int firstMentionEndTokenIndex = mention2
							.getLastTokenIndexInSentence(sameSentence);

					int secondMentionStartTokenIndex = mention1
							.getStartTokenIndexInSentence(sameSentence);

					List<Token> sentTokens = sameSentence.getTokens();
					for (int i = firstMentionEndTokenIndex
							+ 1; i < secondMentionStartTokenIndex; i++)
						tokens.add(sentTokens.get(i));
				}
			} else {
				Sentence firstSentence = mention2.getSentence(ab);
				int firstMentionEndTokenIndex = mention2
						.getLastTokenIndexInSentence(firstSentence);
				List<Token> firstSentTokens = firstSentence.getTokens();
				for (int i = firstMentionEndTokenIndex + 1; i < firstSentTokens
						.size(); i++)
					tokens.add(firstSentTokens.get(i));

				for (int i = mention2.getSentenceIndex() + 1; i < mention1
						.getSentenceIndex(); i++) {
					List<Token> middleSentTokens = sentences.get(i).getTokens();
					tokens.addAll(middleSentTokens);
				}

				Sentence lastSentence = mention1.getSentence(ab);
				int mentionStartTokenIndex = mention1
						.getStartTokenIndexInSentence(lastSentence);
				List<Token> lastSentTokens = lastSentence.getTokens();
				for (int i = 0; i < mentionStartTokenIndex; i++)
					tokens.add(lastSentTokens.get(i));
			}
		}

		return Collections.unmodifiableList(tokens);
	}

	public List<Token> getTokensBetweenTwoNearestMentions() {
		List<Token> tokens = null;
		List<Mention> twoMentions = getTheNearestMentions();
		tokens = getTokensBetweenTwoMentions(twoMentions.get(0),
				twoMentions.get(1));

		return Collections.unmodifiableList(tokens);
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}

	public String getChemicalConceptID() {
		return chemicalConceptID;
	}

	public void setChemicalConceptID(String chemicalConceptID) {
		this.chemicalConceptID = chemicalConceptID;
	}

	public String getDiseaseConceptID() {
		return diseaseConceptID;
	}

	public void setDiseaseConceptID(String diseaseConceptID) {
		this.diseaseConceptID = diseaseConceptID;
	}

	@Override
	public String toString() {
		return "Relation [documentID=" + documentID + ", relationType="
				+ relationType + ", chemicalConceptID=" + chemicalConceptID
				+ ", diseaseConceptID=" + diseaseConceptID + ", probability="
				+ probability + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chemicalConceptID == null) ? 0
				: chemicalConceptID.hashCode());
		result = prime * result + ((diseaseConceptID == null) ? 0
				: diseaseConceptID.hashCode());
		result = prime * result
				+ ((documentID == null) ? 0 : documentID.hashCode());
		result = prime * result
				+ ((relationType == null) ? 0 : relationType.hashCode());
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
		Relation other = (Relation) obj;
		if (chemicalConceptID == null) {
			if (other.chemicalConceptID != null)
				return false;
		} else if (!chemicalConceptID.equals(other.chemicalConceptID))
			return false;
		if (diseaseConceptID == null) {
			if (other.diseaseConceptID != null)
				return false;
		} else if (!diseaseConceptID.equals(other.diseaseConceptID))
			return false;
		if (documentID == null) {
			if (other.documentID != null)
				return false;
		} else if (!documentID.equals(other.documentID))
			return false;
		if (relationType == null) {
			if (other.relationType != null)
				return false;
		} else if (!relationType.equals(other.relationType))
			return false;
		return true;
	}

	public Set<Mention> getChemicalMentions() {
		Coreference coref = getChemicalCoreference();
		if (coref == null)
			return null;

		Set<Mention> mentions = coref.getMentions();

		return Collections.unmodifiableSet(mentions);
	}

	public Set<Mention> getDiseaseMentions() {
		Coreference coref = getDiseaseCoreference();
		if (coref == null)
			return null;

		Set<Mention> mentions = coref.getMentions();

		return mentions;
	}

	public boolean canRelationGetCorrespondingMentions() {
		Set<Mention> chemicals = getChemicalMentions();
		Set<Mention> diseases = getDiseaseMentions();

		if (chemicals == null || diseases == null)
			return false;
		return true;
	}

	@Override
	public int compareTo(Relation o) {
		String thisString = this.documentID + this.chemicalConceptID
				+ this.diseaseConceptID + this.relationType;
		String otherString = o.documentID + o.chemicalConceptID
				+ o.diseaseConceptID + o.relationType;
		return thisString.compareTo(otherString);
	}

}
