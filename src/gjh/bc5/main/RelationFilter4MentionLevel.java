package gjh.bc5.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gjh.bc5.mesh.MeSHDiscriptorRecord;
import gjh.bc5.mesh.MeshDataset;
import gjh.bc5.utils.Relation;

// relation instance filter for mention level
public class RelationFilter4MentionLevel {

	public static boolean isFirstConceptBorderThanSecondConcept(
			String fstConceptID, String sndConceptID, MeshDataset msh) {
		Map<String, MeSHDiscriptorRecord> discriptorUI_2_discriptor_map = msh
				.getUI2DiscriptorMap();

		MeSHDiscriptorRecord fstDiscriptor = discriptorUI_2_discriptor_map
				.get(fstConceptID);

		if (fstDiscriptor == null)
			return false;
		List<String> fstConcept_treeNums = fstDiscriptor.getTreeNumberList();
		if (fstConcept_treeNums == null || fstConcept_treeNums.isEmpty())
			return false;

		MeSHDiscriptorRecord sndDiscriptor = discriptorUI_2_discriptor_map
				.get(sndConceptID);
		if (sndDiscriptor == null)
			return false;
		List<String> sndConcept_treeNums = sndDiscriptor.getTreeNumberList();
		if (sndConcept_treeNums == null || sndConcept_treeNums.isEmpty())
			return false;

		for (String fstTree : fstConcept_treeNums) {
			for (String sndTree : sndConcept_treeNums) {
				if (!fstTree.equals(sndTree)
						&& sndTree.indexOf(fstTree) != -1) {
					return true;
				}
			}
		}
		return false;
	}

	private static List<Relation> filterTrainingSetRelationsByMeSH_v2(
			List<Relation> training_relations, MeshDataset msh) {
		List<Relation> after_filter_hyponymy_relations_list = new ArrayList<Relation>();

		Map<String, List<Relation>> docID_2_positive_rels = new HashMap<String, List<Relation>>();
		Map<String, List<Relation>> docID_2_negative_rels = new HashMap<String, List<Relation>>();

		for (Relation r : training_relations) {
			String docid = r.getDocumentID();

			if (r.getRelationType().equalsIgnoreCase("CID")) {// positive
				if (docID_2_positive_rels.containsKey(docid)) {
					docID_2_positive_rels.get(docid).add(r);
				} else {
					List<Relation> relations = new ArrayList<Relation>();
					relations.add(r);
					docID_2_positive_rels.put(docid, relations);
				}
			} else {// negative
				if (docID_2_negative_rels.containsKey(docid)) {
					docID_2_negative_rels.get(docid).add(r);
				} else {
					List<Relation> relations = new ArrayList<Relation>();
					relations.add(r);
					docID_2_negative_rels.put(docid, relations);
				}
			}
		}

		for (Relation r : training_relations) {
			String docid = r.getDocumentID();
			String relType = r.getRelationType();

			if (relType.equalsIgnoreCase("CID")) {
				after_filter_hyponymy_relations_list.add(r);
			} else {
				String this_chemID = r.getChemicalConceptID();
				String this_disID = r.getDiseaseConceptID();
				boolean is_border_than_others = false;

				List<Relation> thisDoc_positive_relations = docID_2_positive_rels
						.get(docid);

				if (this_disID.equals("D064420"))
					continue;

				if (thisDoc_positive_relations == null) {
					after_filter_hyponymy_relations_list.add(r);
					continue;
				}

				for (Relation other_rel : thisDoc_positive_relations) {
					String other_chem_conceptID = other_rel
							.getChemicalConceptID();
					String other_dis_conceptID = other_rel
							.getDiseaseConceptID();

					if (this_chemID.equals(other_chem_conceptID)) {
						if (isFirstConceptBorderThanSecondConcept(this_disID,
								other_dis_conceptID, msh)) {
							is_border_than_others = true;

							System.out.println("\n\nDisease Hyponymy:");
							System.out.println("\t" + r);
							System.out.println("\t" + other_rel);
						}
					}
					if (this_disID.equals(other_dis_conceptID)) {
						if (isFirstConceptBorderThanSecondConcept(this_chemID,
								other_chem_conceptID, msh)) {
							is_border_than_others = true;

							System.out.println("\n\nChemical Hyponymy:");
							System.out.println("\t" + r);
							System.out.println("\t" + other_rel);
						}
					}
				}

				if (!is_border_than_others) {
					after_filter_hyponymy_relations_list.add(r);
				} else {
					if (r.hasCooccurrenceInOneSentence()) {
						r.setRelationType("CID");
						after_filter_hyponymy_relations_list.add(r);
					}
				}
			}
		}

		return after_filter_hyponymy_relations_list;
	}

	public static List<Relation> filterTrainingSetRelationsByMeSH(
			List<Relation> training_relations, MeshDataset msh) {
		List<Relation> after_filter_hyponymy_relations_list = new ArrayList<Relation>();

		Map<String, List<Relation>> docID_2_positive_rels = new HashMap<String, List<Relation>>();
		Map<String, List<Relation>> docID_2_negative_rels = new HashMap<String, List<Relation>>();

		for (Relation r : training_relations) {
			String docid = r.getDocumentID();

			if (r.getRelationType().equalsIgnoreCase("CID")) {// positive
				if (docID_2_positive_rels.containsKey(docid)) {
					docID_2_positive_rels.get(docid).add(r);
				} else {
					List<Relation> relations = new ArrayList<Relation>();
					relations.add(r);
					docID_2_positive_rels.put(docid, relations);
				}
			} else {// negative
				if (docID_2_negative_rels.containsKey(docid)) {
					docID_2_negative_rels.get(docid).add(r);
				} else {
					List<Relation> relations = new ArrayList<Relation>();
					relations.add(r);
					docID_2_negative_rels.put(docid, relations);
				}
			}
		}

		for (Relation r : training_relations) {
			String docid = r.getDocumentID();
			String relType = r.getRelationType();

			if (relType.equalsIgnoreCase("CID")) {
				after_filter_hyponymy_relations_list.add(r);
			} else {
				String this_chemID = r.getChemicalConceptID();
				String this_disID = r.getDiseaseConceptID();
				boolean is_border_than_others = false;

				List<Relation> thisDoc_positive_relations = docID_2_positive_rels
						.get(docid);

				if (this_disID.equals("D064420"))
					continue;

				if (thisDoc_positive_relations == null) {
					after_filter_hyponymy_relations_list.add(r);
					continue;
				}

				for (Relation other_rel : thisDoc_positive_relations) {
					String other_chem_conceptID = other_rel
							.getChemicalConceptID();
					String other_dis_conceptID = other_rel
							.getDiseaseConceptID();

					if (this_chemID.equals(other_chem_conceptID)) {
						if (isFirstConceptBorderThanSecondConcept(this_disID,
								other_dis_conceptID, msh)) {
							is_border_than_others = true;

							System.out.println("\n\nDisease Hyponymy:");
							System.out.println("\t" + r);
							System.out.println("\t" + other_rel);
						}
					}
					if (this_disID.equals(other_dis_conceptID)) {
						if (isFirstConceptBorderThanSecondConcept(this_chemID,
								other_chem_conceptID, msh)) {
							is_border_than_others = true;

							System.out.println("\n\nChemical Hyponymy:");
							System.out.println("\t" + r);
							System.out.println("\t" + other_rel);
						}
					}
				}

				if (!is_border_than_others) {
					after_filter_hyponymy_relations_list.add(r);
				}
			}
		}

		return after_filter_hyponymy_relations_list;
	}

	public static List<Relation> filterTestSetRelationsByMeSH(
			List<Relation> testSet_relations, MeshDataset msh) {
		List<Relation> rels = new ArrayList<Relation>();
		for (Relation r : testSet_relations) {
			String chemID = r.getChemicalConceptID();
			String disID = r.getDiseaseConceptID();

			if (disID.equals("D064420") || chemID.equals("D004071"))
				continue;

			rels.add(r);
		}

		return rels;
	}
}
