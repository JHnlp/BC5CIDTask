package gjh.bc5.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * 
 * @author GJH
 * 
 */
public class Abstract extends SerialCloneable implements Comparable<Abstract> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9085916550040937162L;
	private String documentID;
	// private List<Tag> tags;
	private String titleText;
	private String abText;
	private List<Sentence> sentences; // sentences[0] is the "Title + white
										// space"
	private List<Mention> mentions;
	private Map<String, Coreference> diseaseCoreferences;
	private Map<String, Coreference> chemicalCoreferences;
	private List<Relation> relations;

	public Abstract(String id, String title, String text,
			List<Sentence> sentences, List<Mention> mentions,
			Map<String, Coreference> chemicalCoreferences,
			Map<String, Coreference> diseaseCoreferences,
			List<Relation> relations) {
		if (id == null)
			throw new IllegalArgumentException("id cannot be null");
		this.documentID = id;

		if (title == null)
			throw new IllegalArgumentException("title cannot be null");
		this.titleText = title;

		if (text == null)
			throw new IllegalArgumentException("text cannot be null");
		this.abText = text;

		if (sentences == null)
			this.sentences = new ArrayList<Sentence>();
		else
			this.sentences = sentences;

		if (mentions == null)
			this.mentions = new ArrayList<Mention>();
		else
			this.mentions = mentions;

		if (chemicalCoreferences == null)
			this.chemicalCoreferences = new HashMap<String, Coreference>();
		else
			this.chemicalCoreferences = chemicalCoreferences;

		if (diseaseCoreferences == null)
			this.diseaseCoreferences = new HashMap<String, Coreference>();
		else
			this.diseaseCoreferences = diseaseCoreferences;

		if (relations == null)
			this.relations = new ArrayList<Relation>();
		else
			this.relations = relations;
	}

	public Abstract(String id, String title, String text) {
		// Empty
		this(id, title, text, null, null, null, null, null);
	}

	public String getTitleText() {
		return titleText;
	}

	public String getAbstractText() {
		return abText;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((documentID == null) ? 0 : documentID.hashCode());
		result = prime * result + ((abText == null) ? 0 : abText.hashCode());
		result = prime * result
				+ ((titleText == null) ? 0 : titleText.hashCode());
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
		Abstract other = (Abstract) obj;
		if (documentID == null) {
			if (other.documentID != null)
				return false;
		} else if (!documentID.equals(other.documentID))
			return false;
		if (abText == null) {
			if (other.abText != null)
				return false;
		} else if (!abText.equals(other.abText))
			return false;
		if (titleText == null) {
			if (other.titleText != null)
				return false;
		} else if (!titleText.equals(other.titleText))
			return false;
		return true;
	}

	public String getWholeText() {
		return titleText + " " + abText;
	}

	public void setAbstractText(String abText) {
		this.abText = abText;
	}

	public void setTitleText(String title) {
		this.titleText = title;
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String id) {
		this.documentID = id;
	}

	public List<Sentence> getSentences() {
		return Collections.unmodifiableList(sentences);
	}

	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}

	public List<Mention> getMentions() {
		return Collections.unmodifiableList(mentions);
	}

	public void setMentions(List<Mention> mentions) {
		this.mentions = mentions;
	}

	public Map<String, Coreference> getDiseaseCoreferences() {
		return Collections.unmodifiableMap(diseaseCoreferences);
	}

	public void setDiseaseCoreferences(
			Map<String, Coreference> diseaseCoreferences) {
		this.diseaseCoreferences = diseaseCoreferences;
	}

	public Map<String, Coreference> getChemicalCoreferences() {
		return Collections.unmodifiableMap(chemicalCoreferences);
	}

	public void setChemicalCoreferences(
			Map<String, Coreference> chemicalCoreferences) {
		this.chemicalCoreferences = chemicalCoreferences;
	}

	public List<Relation> getRelations() {
		return Collections.unmodifiableList(relations);
	}

	public void setRelations(List<Relation> relations) {
		this.relations = relations;
	}

	public boolean addSentence(Sentence sentence) {
		return this.sentences.add(sentence);
	}

	public boolean addMention(Mention mention) {
		return this.mentions.add(mention);
	}

	public boolean addRelation(Relation relation) {
		return this.relations.add(relation);
	}

	public boolean addChemicalMention2Coreferences(Mention m) {
		if (!m.getDocumentID().equals(getDocumentID()))
			throw new IllegalArgumentException("documentID  does not match!");
		if (!m.getEntityType().getTypeName().equals("Chemical"))
			throw new IllegalArgumentException("EntityType  does not match!");

		String concptId = m.getConceptID();
		if (this.chemicalCoreferences.containsKey(concptId)) {
			return this.chemicalCoreferences.get(concptId).addMention(m);
		} else {
			chemicalCoreferences.put(concptId,
					new Coreference(documentID, concptId, m));
			return true;
		}
	}

	public boolean addDiseaseMention2Coreferences(Mention m) {
		if (!m.getDocumentID().equals(getDocumentID()))
			throw new IllegalArgumentException("documentID  does not match!");
		if (!m.getEntityType().getTypeName().equals("Disease"))
			throw new IllegalArgumentException("EntityType  does not match!");

		String concptId = m.getConceptID();
		if (this.diseaseCoreferences.containsKey(concptId)) {
			return this.diseaseCoreferences.get(concptId).addMention(m);
		} else {
			diseaseCoreferences.put(concptId,
					new Coreference(documentID, concptId, m));
			return true;
		}
	}

	public boolean addSentences(List<Sentence> sentences) {
		return this.sentences.addAll(sentences);
	}

	public boolean addMentions(List<Mention> mentions) {
		return this.mentions.addAll(mentions);
	}

	public boolean addRelations(List<Relation> relations) {
		boolean state = false;
		for (Relation r : relations) {
			r.setAbstract(this);
			state = this.relations.add(r);
		}
		return state;
	}

	public void addAllMentions2Coreferences() {
		for (Mention m : this.mentions) {
			if (m.getEntityType().getTypeName().equals("Chemical")) {
				addChemicalMention2Coreferences(m);
			}
			if (m.getEntityType().getTypeName().equals("Disease")) {
				addDiseaseMention2Coreferences(m);
			}
		}
	}

	public Set<String> getChemicalConceptIDs() {
		return Collections.unmodifiableSet(chemicalCoreferences.keySet());
	}

	public Set<String> getDiseaseConceptIDs() {
		return Collections.unmodifiableSet(diseaseCoreferences.keySet());
	}

	public String getSpecificText(int start_sentIndex, int last_sentIndex) {
		List<Sentence> sentences = getSentences();
		if (start_sentIndex < 0 || last_sentIndex >= sentences.size()
				|| start_sentIndex > last_sentIndex)
			throw new IllegalArgumentException("text span error!");

		String text = "";
		for (int i = start_sentIndex; i <= last_sentIndex; i++) {
			if (i == last_sentIndex) {
				text += sentences.get(i).getText();
			} else {
				text += sentences.get(i).getText() + " ";
			}
		}
		return text;
	}

	// the first version of the available corpus has several annotation errors,
	// so it needs filtering
	public List<Relation> formNewRelations() {
		List<Relation> rels = new ArrayList<Relation>();

		Set<String> relationKeys = new TreeSet<String>();
		for (Relation r : this.relations) {
			if (!r.canRelationGetCorrespondingMentions())
				continue;

			String chemID = r.getChemicalConceptID();
			String disID = r.getDiseaseConceptID();
			relationKeys.add(chemID + "-" + disID);

			rels.add(r);
		}

		Set<String> chemicalIDs = getChemicalConceptIDs();
		Set<String> diseaseIDs = getDiseaseConceptIDs();
		for (String chemicalID : chemicalIDs) {
			if (chemicalID.equals("-1")) {
				continue;
			}

			for (String diseaseID : diseaseIDs) {
				if (diseaseID.equals("-1")) {
					continue;
				}

				String key = chemicalID + "-" + diseaseID;
				if (relationKeys.contains(key)) {
					continue;
				} else {
					rels.add(new Relation(documentID, this, "UN", chemicalID,
							diseaseID));
				}
			}
		}

		Collections.sort(rels);
		return rels;
	}

	@Override
	public int compareTo(Abstract o) {
		return this.documentID.compareTo(o.documentID);
	}

	// for stanford corenlp pipeline processing
	public Annotation getAnnotionStructure() {
		List<CoreMap> sentencesInStanford = new ArrayList<CoreMap>();
		for (Sentence s : this.sentences) {
			Annotation sentAnnotation = s.getStanfordSentenceAnnotation();
			sentencesInStanford.add(sentAnnotation);
		}

		Annotation anno = new Annotation(sentencesInStanford);
		return anno;
	}

	@Override
	public String toString() {

		String str = "";
		str += documentID + "|t|" + titleText + "\n";
		str += documentID + "|a|" + abText + "\n";

		for (int i = 0; i < mentions.size(); i++) {
			Mention m = mentions.get(i);
			int startOffInDoc = m.getStartOffsetInDocument();
			int endOffInDoc = m.getEndOffsetInDocument();
			String menText = m.getText();
			String type = m.getEntityType().getTypeName();
			String conceptID = m.getConceptID();

			str += documentID + "\t" + startOffInDoc + "\t" + endOffInDoc + "\t"
					+ menText + "\t" + type + "\t" + conceptID + "\n";
		}

		for (int i = 0; i < relations.size(); i++) {
			Relation r = relations.get(i);
			String chemConceptID = r.getChemicalConceptID();
			String disConceptID = r.getDiseaseConceptID();
			double probability = r.getProbability();

			str += documentID + "\tCID\t" + chemConceptID + "\t" + disConceptID
					+ "\t" + probability + "\n";
		}

		return str;
	}

}
