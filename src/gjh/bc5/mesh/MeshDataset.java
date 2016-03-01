package gjh.bc5.mesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MeshDataset {
	private List<MeSHDiscriptorRecord> mesh_discriptor_records;
	private List<MeSHSCRecord> mesh_scr_records;

	private Map<String, String> treeNum_2_discriptorUI_map;
	private Map<String, String> heading_2_discriptorUI_map;
	private Map<String, MeSHDiscriptorRecord> discriptorUI_2_discriptor_map;

	private Map<String, String> heading_2_SCRUI_map;
	private Map<String, MeSHSCRecord> scrUI_2_supplementary_concept_map;

	private static final String DISCRIPTOR_FILEPATH = "MeSH/Descriptor Records 2015.txt";
	private static final String SUPPLEMENTARY_CONCEPT_RECORD_FILEPATH = "MeSH/Supplementary Concept Records 2015.txt";
	// private String qualifier_filepath;

	public MeshDataset() {
		this(DISCRIPTOR_FILEPATH, SUPPLEMENTARY_CONCEPT_RECORD_FILEPATH);
	}

	public MeshDataset(String discriptor_filepath,
			String supplementary_concept_record_filepath) {
		super();
		mesh_discriptor_records = new ArrayList<MeSHDiscriptorRecord>();
		treeNum_2_discriptorUI_map = new HashMap<String, String>();
		heading_2_discriptorUI_map = new HashMap<String, String>();
		discriptorUI_2_discriptor_map = new HashMap<String, MeSHDiscriptorRecord>();

		mesh_scr_records = new ArrayList<MeSHSCRecord>();
		heading_2_SCRUI_map = new HashMap<String, String>();
		scrUI_2_supplementary_concept_map = new HashMap<String, MeSHSCRecord>();

		loadMeSHDiscriptorRecords(discriptor_filepath);
		loadMeSHSupplementaryConceptRecords(
				supplementary_concept_record_filepath);

	}

	public List<MeSHDiscriptorRecord> getDiscriptorRecords() {
		return Collections.unmodifiableList(mesh_discriptor_records);
	}

	public Map<String, String> getTreeNum2DiscriptorUIMap() {
		return Collections.unmodifiableMap(treeNum_2_discriptorUI_map);
	}

	public Map<String, String> getHeading2DiscriptorMap() {
		return Collections.unmodifiableMap(heading_2_discriptorUI_map);
	}

	public Map<String, MeSHDiscriptorRecord> getUI2DiscriptorMap() {
		return Collections.unmodifiableMap(discriptorUI_2_discriptor_map);
	}

	// "MeSH/Descriptor Records 2015.txt"
	public void loadMeSHDiscriptorRecords(String discriptor_records_filePath) {
		File discriptor_file = new File(discriptor_records_filePath);
		if (!discriptor_file.exists() || !discriptor_file.isFile())
			throw new IllegalArgumentException("Mesh Descriptor File error!");

		Scanner in = null;
		ArrayDeque<String> discriptor_cache = new ArrayDeque<String>();
		try {
			try {
				in = new Scanner(discriptor_file, "utf-8");
				while (in.hasNextLine()) {
					String line = in.nextLine();
					// System.out.println(word);

					if (line.isEmpty()) {
						// construct a MeSHDiscriptorRecord
						String mesh_heading = null; // MH, this the heading name
						String record_type = null;// RECTYPE
						String descriptor_class = null;// DC
						List<String> allowable_qualifier_list = null;// AQ
						String annotation = null; // AN
						String mesh_scope_note = null;// MS
						List<String> semantic_type_list = null;// ST
						String unique_identifier = null;// UI
						List<String> pharmacological_action_list = null;// PA
						List<String> tree_number_list = null;// MN
						List<String> entry_names = null;// ENTRY or PRINT ENTRY

						while (!discriptor_cache.isEmpty()) {
							String entry_line = discriptor_cache.poll();
							// System.out.println(entry_line);

							if (entry_line.indexOf("RECTYPE = ") != -1)
								record_type = entry_line.substring(10);

							if (entry_line.indexOf("MH = ") != -1)
								mesh_heading = entry_line.substring(5);

							if (entry_line.indexOf("DC = ") != -1)
								descriptor_class = entry_line.substring(5);

							if (entry_line.indexOf("AQ = ") != -1) {
								String qualifiers_sequence = entry_line
										.substring(5);
								String[] qualifiers = qualifiers_sequence
										.split(" ");

								allowable_qualifier_list = new ArrayList<String>();
								for (String q : qualifiers)
									allowable_qualifier_list.add(q);

							}

							if (entry_line.indexOf("AN = ") != -1)
								annotation = entry_line.substring(5);

							if (entry_line.indexOf("MS = ") != -1)
								mesh_scope_note = entry_line.substring(5);

							if (entry_line.indexOf("ST = ") != -1) {
								if (semantic_type_list == null) {
									semantic_type_list = new ArrayList<String>();
									semantic_type_list
											.add(entry_line.substring(5));
								} else {
									semantic_type_list
											.add(entry_line.substring(5));
								}
							}

							if (entry_line.indexOf("UI = ") != -1)
								unique_identifier = entry_line.substring(5);

							if (entry_line.indexOf("PA = ") != -1) {
								if (pharmacological_action_list == null) {
									pharmacological_action_list = new ArrayList<String>();
									pharmacological_action_list
											.add(entry_line.substring(5));
								} else {
									pharmacological_action_list
											.add(entry_line.substring(5));
								}
							}

							if (entry_line.indexOf("MN = ") != -1) {
								if (tree_number_list == null) {
									tree_number_list = new ArrayList<String>();
									tree_number_list
											.add(entry_line.substring(5));
								} else {
									tree_number_list
											.add(entry_line.substring(5));
								}
							}

							if (entry_line.indexOf("PRINT ENTRY = ") != -1) {
								String name_line = entry_line.substring(14);
								String[] splits = name_line.split("\\|");
								if (entry_names == null) {
									entry_names = new ArrayList<String>();

									entry_names.add(splits[0]);
								} else {
									entry_names.add(splits[0]);
								}
							}

							if (entry_line.indexOf("PRINT ENTRY = ") == -1
									&& entry_line.indexOf("ENTRY = ") != -1) {
								String name_line = entry_line.substring(8);
								String[] splits = name_line.split("\\|");

								if (entry_names == null) {
									entry_names = new ArrayList<String>();
									entry_names.add(splits[0]);
								} else {
									entry_names.add(splits[0]);
								}
							}

						}

						MeSHDiscriptorRecord mDisRec = new MeSHDiscriptorRecord(
								mesh_heading, record_type, descriptor_class,
								allowable_qualifier_list, annotation,
								mesh_scope_note, semantic_type_list,
								unique_identifier, pharmacological_action_list,
								tree_number_list, entry_names);

						this.mesh_discriptor_records.add(mDisRec);

						if (tree_number_list != null) {
							for (String tree_num : tree_number_list) {
								if (this.treeNum_2_discriptorUI_map
										.containsKey(tree_num)) {
									System.out.println(
											"\none tree num map to multiple ui:");
									System.out.println("\t"
											+ this.treeNum_2_discriptorUI_map
													.get(tree_num)
											+ "\t" + unique_identifier);
								} else {
									this.treeNum_2_discriptorUI_map
											.put(tree_num, unique_identifier);
								}
							}
						}
						this.heading_2_discriptorUI_map.put(mesh_heading,
								unique_identifier);
						this.discriptorUI_2_discriptor_map
								.put(unique_identifier, mDisRec);
					} else {
						discriptor_cache.add(line);
					}
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// "MeSH/Descriptor Records 2015.txt"
	public void loadMeSHSupplementaryConceptRecords(
			String supplementary_concept_record_filepath) {
		File scr_file = new File(supplementary_concept_record_filepath);
		if (!scr_file.exists() || !scr_file.isFile())
			throw new IllegalArgumentException(
					"Mesh Supplementary Concept File error!");

		Scanner in = null;
		ArrayDeque<String> scr_cache = new ArrayDeque<String>();
		try {
			try {
				in = new Scanner(scr_file, "utf-8");
				while (in.hasNextLine()) {
					String line = in.nextLine();
					// System.out.println(word);

					if (line.isEmpty()) {
						// construct a MeSHSCRecord
						Integer frequency = null; // FR, possible null
						List<String> heading_mapped_to = null;// HM
						String indexing_information = null;// II
						String name_of_substance = null;// NM, this the heading
														// name
						List<String> pharmacological_action_list = null;// PA
						String record_type = null;// RECTYPE
						List<String> semantic_type_list = null;// ST
						List<String> synonyms = null;// SY
						String unique_identifier = null;// UI

						while (!scr_cache.isEmpty()) {
							String entry_line = scr_cache.poll();
							// System.out.println(entry_line);

							if (entry_line.indexOf("FR = ") != -1)
								frequency = Integer
										.valueOf(entry_line.substring(5));

							if (entry_line.indexOf("HM = ") != -1) {
								if (heading_mapped_to == null) {
									heading_mapped_to = new ArrayList<String>();
									heading_mapped_to
											.add(entry_line.substring(5));
								} else {
									heading_mapped_to
											.add(entry_line.substring(5));
								}
							}

							if (entry_line.indexOf("II = ") != -1)
								indexing_information = entry_line.substring(5);

							if (entry_line.indexOf("NM = ") != -1)
								name_of_substance = entry_line.substring(5);

							if (entry_line.indexOf("PA = ") != -1) {
								if (pharmacological_action_list == null) {
									pharmacological_action_list = new ArrayList<String>();
									pharmacological_action_list
											.add(entry_line.substring(5));
								} else {
									pharmacological_action_list
											.add(entry_line.substring(5));
								}
							}

							if (entry_line.indexOf("RECTYPE = ") != -1)
								record_type = entry_line.substring(10);

							if (entry_line.indexOf("ST = ") != -1) {
								if (semantic_type_list == null) {
									semantic_type_list = new ArrayList<String>();
									semantic_type_list
											.add(entry_line.substring(5));
								} else {
									semantic_type_list
											.add(entry_line.substring(5));
								}
							}

							if (entry_line.indexOf("SY = ") != -1) {
								String name_line = entry_line.substring(5);
								String[] splits = name_line.split("\\|");
								if (synonyms == null) {
									synonyms = new ArrayList<String>();

									synonyms.add(splits[0]);
								} else {
									synonyms.add(splits[0]);
								}
							}

							if (entry_line.indexOf("UI = ") != -1)
								unique_identifier = entry_line.substring(5);

						}

						MeSHSCRecord scr_record = new MeSHSCRecord(frequency,
								heading_mapped_to, indexing_information,
								name_of_substance, pharmacological_action_list,
								record_type, semantic_type_list, synonyms,
								unique_identifier);

						this.mesh_scr_records.add(scr_record);
						this.heading_2_SCRUI_map.put(name_of_substance,
								unique_identifier);
						this.scrUI_2_supplementary_concept_map
								.put(unique_identifier, scr_record);
					} else {
						scr_cache.add(line);
					}
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public MeSHDiscriptorRecord getDiscriptorRecord(String ui) {
		return getUI2DiscriptorMap().get(ui);
	}

	public List<String> getDiscriptorRootTreeNameByUI(String ui) {
		List<String> treeNums = getUI2DiscriptorMap().get(ui)
				.getTreeNumberList();
		List<String> root_tree_names = new ArrayList<String>();

		if (treeNums == null)
			return null;

		for (String tree_num : treeNums) {
			String[] splits = tree_num.split("\\.");

			String root_num = splits[0];
			String root_ui = getTreeNum2DiscriptorUIMap().get(root_num);
			MeSHDiscriptorRecord mdr = getUI2DiscriptorMap().get(root_ui);
			root_tree_names.add(mdr.getMeshHeadingName());
		}

		if (root_tree_names.isEmpty())
			return null;
		return root_tree_names;
	}

	// public void fffffffff(String path) {
	//
	// Scanner in = null;
	//
	// Set<String> chem_root_names = new HashSet<String>();
	// Set<String> dis_root_names = new HashSet<String>();
	// try {
	// try {
	// in = new Scanner(new File(path), "utf-8");
	// while (in.hasNextLine()) {
	// String line = in.nextLine();
	// String[] splits = line.split("\\t");
	// String chemID = splits[2];
	// String disID = splits[3];
	//
	// List<String> chem_roots = getDiscriptorRootTreeName(chemID);
	//
	// List<String> dis_roots = getDiscriptorRootTreeName(disID);
	// if (dis_roots != null && dis_roots.size() == 1 && dis_roots
	// .get(0).equals("Chemically-Induced Disorders"))
	// System.out.println(disID);
	//
	// if (chem_roots != null && !chem_roots.isEmpty())
	// chem_root_names.addAll(chem_roots);
	//
	// if (dis_roots != null && !dis_roots.isEmpty())
	// dis_root_names.addAll(dis_roots);
	//
	// }
	//
	// System.out.println("\nChemical Tree Root:");
	// for (String ch : chem_root_names)
	// System.out.println("\t" + ch);
	//
	// System.out.println("\n\nDisease Tree Root:");
	// for (String di : dis_root_names)
	// System.out.println("\t" + di);
	//
	// } finally {
	// in.close();
	// }
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// }
	//
	// }

	public static void main(String[] args) {
		// MeshDataset md = new MeshDataset();

		// md.fffffffff("DifferentMethods/SplitProcessing_MentionLevel/dataset/CDR_TrainingSet.PubTator.txt.relation");
		// md.fffffffff("DifferentMethods/SplitProcessing_MentionLevel/dataset/CDR_DevelopmentSet.PubTator.txt.relation");
		// md.fffffffff(
		// "DifferentMethods/SplitProcessing_MentionLevel/dataset/CDR_TestSet.PubTator.txt.relation");

		// md.fffffffff("DifferentMethods/SplitProcessing_MentionLevel/process2/gjh_CID_result.txt");
		System.out.println("\n\n**** End ****");
	}
}
