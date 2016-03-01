package gjh.bc5.main;

import gjh.bc5.dataset.GJHPubtatorDataset;
import gjh.bc5.features.FeatureSet;
import gjh.bc5.features.MalletClassificationComparator;
import gjh.bc5.features.MentionPairAsInstanceSource;
import gjh.bc5.mesh.MeshDataset;
import gjh.bc5.features.FeatureSet.FeatrueConstructionMethod;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Sentence;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;

/**
 * 
 * @author GJH
 * 
 */
public class BC5Runner implements Serializable {

	private static final long serialVersionUID = -5325306701196684606L;

	public static final Set<String> STOPWORDS_SET = loadStopWords();
	public static final MeshDataset MeSH_CONTROLLED_VOCABULARIES = new MeshDataset();

	public static MeshDataset getMeshThesaurus() {
		return MeSH_CONTROLLED_VOCABULARIES;
	}

	public static Set<String> loadStopWords() {
		File stopWordsFile = new File("english_stopwords.tsv");
		if (!stopWordsFile.exists())
			throw new IllegalStateException("Unknown stopwords file!");

		Set<String> stopWords = new HashSet<String>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(stopWordsFile, "utf-8");
				while (in.hasNextLine()) {
					String word = in.nextLine();
					// System.out.println(word);
					if (word != null && !word.isEmpty())
						stopWords.add(word);
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return Collections.unmodifiableSet(stopWords);
	}

	public static Set<String> getStopWords() {
		return STOPWORDS_SET;
	}

	// Train a classifier
	public static Classifier trainClassifier(InstanceList trainingInstances) {
		// Here we use a maximum entropy (ie polytomous logistic regression)
		// classifier.
		ClassifierTrainer trainer = new MaxEntTrainer();
		return trainer.train(trainingInstances);
	}

	public static void writeCIDRecognitionResult_MentionLevel(
			File output_directory, Trial trial_Cooccur, Trial trial_Uncooccur) {
		PrintWriter outCIDRecognition = null;
		try {
			try {
				outCIDRecognition = new PrintWriter(
						output_directory.getPath() + "/" + "CID_result.txt",
						"utf-8");

				// cooccur
				Map<String, List<Double>> ifPositive_Cooccur = new TreeMap<String, List<Double>>();
				Map<String, List<Double>> ifNegtive_Cooccur = new TreeMap<String, List<Double>>();
				for (int i = 0; i < trial_Cooccur.size(); i++) {
					Classification cl = trial_Cooccur.get(i);
					Instance instance = cl.getInstance();
					Label classified_label = cl.getLabeling().getBestLabel();
					double probability = cl.getLabelVector().getBestValue();
					MentionPairAsInstanceSource source = (MentionPairAsInstanceSource) instance
							.getSource();

					Relation relation = source.getRelationSource();
					Mention chemMention = source.getChemMention();
					String chemConceptID = chemMention.getConceptID();
					Mention disMention = source.getDisMention();
					String disConceptID = disMention.getConceptID();
					String DocID = relation.getDocumentID();

					String key = DocID + "-" + chemConceptID + "-"
							+ disConceptID;

					if (classified_label.toString().equalsIgnoreCase("CID")) {
						if (ifPositive_Cooccur.containsKey(key)) {
							List<Double> probs = ifPositive_Cooccur.get(key);
							probs.add(probability);
						} else {
							List<Double> probs = new ArrayList<Double>();
							probs.add(probability);
							ifPositive_Cooccur.put(key, probs);
						}
					} else {
						if (ifNegtive_Cooccur.containsKey(key)) {
							List<Double> probs = ifNegtive_Cooccur.get(key);
							probs.add(probability);
						} else {
							List<Double> probs = new ArrayList<Double>();
							probs.add(probability);
							ifNegtive_Cooccur.put(key, probs);
						}
					}
				}

				for (Map.Entry<String, List<Double>> rs : ifPositive_Cooccur
						.entrySet()) {
					String key = rs.getKey();
					List<Double> probabilities = rs.getValue();
					Double maxProb = -1.0;
					for (Double d : probabilities) {
						if (maxProb.compareTo(d) < 0)
							maxProb = d;
					}

					String split[] = key.split("\\-");
					String docID = split[0];
					String chemConceptID = split[1];
					String disConceptID = split[2];

					outCIDRecognition.println(docID + "\tCID\t" + chemConceptID
							+ "\t" + disConceptID + "\t" + maxProb);
				}

				// uncooccur
				Map<String, List<Double>> ifPositive_Uncooccur = new TreeMap<String, List<Double>>();
				Map<String, List<Double>> ifNegtive_Uncooccur = new TreeMap<String, List<Double>>();
				for (int i = 0; i < trial_Uncooccur.size(); i++) {
					Classification cl = trial_Uncooccur.get(i);
					Instance instance = cl.getInstance();
					Label classLabel = cl.getLabeling().getBestLabel();
					double probability = cl.getLabelVector().getBestValue();

					MentionPairAsInstanceSource source = (MentionPairAsInstanceSource) instance
							.getSource();

					Relation relation = source.getRelationSource();
					String DocID = relation.getDocumentID();
					String chemConceptID = relation.getChemicalConceptID();
					String disConceptID = relation.getDiseaseConceptID();
					String key = DocID + "-" + chemConceptID + "-"
							+ disConceptID;

					if (classLabel.toString().equalsIgnoreCase("CID")) {
						if (ifPositive_Uncooccur.containsKey(key)) {
							List<Double> probs = ifPositive_Uncooccur.get(key);
							probs.add(probability);
						} else {
							List<Double> probs = new ArrayList<Double>();
							probs.add(probability);
							ifPositive_Uncooccur.put(key, probs);
						}
					} else {
						if (ifNegtive_Uncooccur.containsKey(key)) {
							List<Double> probs = ifNegtive_Uncooccur.get(key);
							probs.add(probability);
						} else {
							List<Double> probs = new ArrayList<Double>();
							probs.add(probability);
							ifNegtive_Uncooccur.put(key, probs);
						}
					}
				}

				for (Map.Entry<String, List<Double>> rs : ifPositive_Uncooccur
						.entrySet()) {
					String key = rs.getKey();
					List<Double> probabilities = rs.getValue();
					Double maxProb = -1.0;
					for (Double d : probabilities) {
						if (maxProb.compareTo(d) < 0)
							maxProb = d;
					}

					String split[] = key.split("\\-");
					String docID = split[0];
					String chemConceptID = split[1];
					String disConceptID = split[2];

					outCIDRecognition.println(docID + "\tCID\t" + chemConceptID
							+ "\t" + disConceptID + "\t" + maxProb);
				}
				outCIDRecognition.flush();
			} finally {
				outCIDRecognition.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static List<String> filterHyponymyResults(List<String> lines,
			MeshDataset msh) {
		List<String> after_filter_hyponymy_results_list = new ArrayList<String>();

		Map<String, List<String>> docID_2_lines_map = new HashMap<String, List<String>>();
		for (String l : lines) {
			String docid = l.substring(0, l.indexOf('\t'));

			if (docID_2_lines_map.containsKey(docid)) {
				docID_2_lines_map.get(docid).add(l);
			} else {
				List<String> list = new ArrayList<String>();
				list.add(l);
				docID_2_lines_map.put(docid, list);
			}
		}

		for (Map.Entry<String, List<String>> entry : docID_2_lines_map
				.entrySet()) {
			List<String> doc_lines = entry.getValue();

			for (int i = 0; i < doc_lines.size(); i++) {
				String l1 = doc_lines.get(i);
				if (l1.isEmpty())
					continue;

				boolean tag = false;

				String[] split1 = l1.split("\\t");
				String chem1_conceptID = split1[2];
				String dis1_conceptID = split1[3];
				// Double pro1 = Double.valueOf(split1[4]);

				for (int j = 0; j < doc_lines.size(); j++) {
					if (j == i)
						continue;

					String l2 = doc_lines.get(j);
					if (l2.isEmpty())
						continue;

					String[] split2 = l2.split("\\t");
					String chem2_conceptID = split2[2];
					String dis2_conceptID = split2[3];
					// Double pro2 = Double.valueOf(split2[4]);

					if (chem1_conceptID.equals(chem2_conceptID)) {
						if (RelationFilter4MentionLevel
								.isFirstConceptBorderThanSecondConcept(
										dis1_conceptID, dis2_conceptID, msh)) {
							tag = true;

							// System.out.println("\n\nDisease Hyponymy:");
							// System.out.println("\t" + l1);
							// System.out.println("\t" + l2);
						}
					}
					if (dis1_conceptID.equals(dis2_conceptID)) {
						if (RelationFilter4MentionLevel
								.isFirstConceptBorderThanSecondConcept(
										chem1_conceptID, chem2_conceptID,
										msh)) {
							tag = true;

							// System.out.println("\n\nChemical Hyponymy:");
							// System.out.println("\t" + l1);
							// System.out.println("\t" + l2);
						}
					}
				}

				if (!tag) {
					after_filter_hyponymy_results_list.add(l1);
				}
			}
		}

		return after_filter_hyponymy_results_list;
	}

	public static void postProcessing(String result_filepath,
			String post_result_filePath, MeshDataset msh) {
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(new File(result_filepath), "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<String> after_lines = filterHyponymyResults(lines, msh);
		try {
			FileUtils.writeLines(new File(post_result_filePath), "utf-8",
					after_lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void evaluate_MentionLevel(Trial trial, File result_directory,
			String type) throws IOException {
		if (type.equalsIgnoreCase("cooccurrence")) {
			System.out.println("The number of instances for classification: "
					+ trial.size());
			System.out.println("Total accuracy: " + trial.getAccuracy());
			System.out.println(
					"Precision for class 'CID': " + trial.getPrecision("CID"));
			System.out.println(
					"Recall for class 'CID': " + trial.getRecall("CID"));
			System.out.println("F1 for class 'CID': " + trial.getF1("CID"));

			// writeClassifyResult(resultDirectory + "/" + "classifyResult.txt",
			// "utf-8", trial, "CID");
			writeClassifyResult(result_directory, "_Cooccur_True_Positive.txt",
					"_Cooccur_False_Positive.txt", "_Cooccur_True_Negative.txt",
					"_Cooccur_False_Negative.txt", "utf-8", trial, "CID");
		} else if (type.equalsIgnoreCase("unCooccurrence")) {
			System.out.println("The number of instances for classification: "
					+ trial.size());
			System.out.println("Total accuracy: " + trial.getAccuracy());
			System.out.println(
					"Precision for class 'CID': " + trial.getPrecision("CID"));
			System.out.println(
					"Recall for class 'CID': " + trial.getRecall("CID"));
			System.out.println("F1 for class 'CID': " + trial.getF1("CID"));

			// writeClassifyResult(resultDirectory + "/" + "classifyResult.txt",
			// "utf-8", trial, "CID");
			writeClassifyResult(result_directory,
					"_Uncooccur_True_Positive.txt",
					"_Uncooccur_False_Positive.txt",
					"_Uncooccur_True_Negative.txt",
					"_Uncooccur_False_Negative.txt", "utf-8", trial, "CID");
		} else {
			throw new IllegalArgumentException(
					"The argument \"type\" is wrong!");
		}
	}

	// output feature vectors of instances
	public static void writeInstanceFeatures(File directory, String filename,
			String encoding, InstanceList instances) {
		if (!directory.exists())
			directory.mkdirs();

		PrintWriter out = null;
		try {
			try {
				out = new PrintWriter(directory.getPath() + "/" + filename,
						encoding);

				for (Instance instance : instances) {
					String name = (String) instance.getName();
					Labeling target = (Labeling) instance.getTarget();
					Label best_label = target.getBestLabel();
					// System.out.println("target: " + target.getClass());
					// System.out.println("targetName: " +
					// targetName.getClass());
					// System.out.println("targetName value: " +
					// targetName.getBestIndex());

					FeatureVector fv = (FeatureVector) instance.getData();
					out.println(best_label + "\t" + name + "\t"
							+ fv.toString(true));
				}
				out.flush();
			} finally {
				out.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static void writeInstanceSource_MentionLevel_Cooccur(File directory,
			String output_filename, String encoding, InstanceList instances) {
		if (!directory.exists()) {
			directory.mkdirs();
		}

		PrintWriter out = null;
		try {
			try {
				out = new PrintWriter(
						directory.getPath() + "/" + output_filename, encoding);

				for (Instance instance : instances) {
					String instanceName = (String) instance.getName();
					Labeling target = (Labeling) instance.getTarget();
					Label targetName = target.getBestLabel();
					// System.out.println("target: " + target.getClass());
					// System.out.println("targetName: " +
					// targetName.getClass());

					MentionPairAsInstanceSource source = (MentionPairAsInstanceSource) instance
							.getSource();
					Mention chemMention = source.getChemMention();
					// Mention disMention = source.getDisMention();
					// Relation relation = source.getRelationSource();
					Abstract a = source.getAbstract();
					Sentence s = chemMention.getSentence(a);

					out.println(targetName + "\t" + instanceName + "\t"
							+ s.getText());
				}
				out.flush();
			} finally {
				out.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static void writeInstanceSource_MentionLevel_Uncooccur(
			File directory, String output_filename, String fileEncoding,
			InstanceList instances) {
		if (!directory.exists()) {
			directory.mkdirs();
		}

		PrintWriter out = null;
		try {
			try {
				out = new PrintWriter(
						directory.getPath() + "/" + output_filename,
						fileEncoding);

				for (Instance instance : instances) {
					String instanceName = (String) instance.getName();
					Labeling target = (Labeling) instance.getTarget();
					Label targetName = target.getBestLabel();
					// System.out.println("target: " + target.getClass());
					// System.out.println("targetName: " +
					// targetName.getClass());

					MentionPairAsInstanceSource source = (MentionPairAsInstanceSource) instance
							.getSource();
					Mention chemMention = source.getChemMention();
					Mention disMention = source.getDisMention();
					Abstract a = source.getAbstract();

					out.print(targetName + "\t" + instanceName + "\t");

					int chemSentIndex = chemMention.getSentenceIndex();
					int disSentIndex = disMention.getSentenceIndex();
					Integer bg = null;
					Integer lt = null;
					if (chemSentIndex < disSentIndex) {
						bg = chemSentIndex;
						lt = disSentIndex;
					} else {
						bg = disSentIndex;
						lt = chemSentIndex;
					}

					out.println("[" + chemSentIndex + ", "
							+ chemMention.getText() + "; " + disSentIndex + ", "
							+ disMention.getText() + "]## "
							+ a.getSpecificText(bg, lt) + "\n");
				}
				out.flush();
			} finally {
				out.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static void writeClassifyResult(File directory,
			String truePositiveFilename, String falsePositiveFilename,
			String trueNegativeFilename, String falseNegativeFilename,
			String fileEncoding, Trial trial, String relationType) {
		if (!directory.exists())
			directory.mkdirs();
		String rootDir = directory.getPath();

		Set<Classification> tpRelationIDs = new TreeSet<Classification>(
				new MalletClassificationComparator()); // true positive
		Set<Classification> fpRelationIDs = new TreeSet<Classification>(
				new MalletClassificationComparator()); // false positive
		Set<Classification> tnRelationIDs = new TreeSet<Classification>(
				new MalletClassificationComparator()); // true negative
		Set<Classification> fnRelationIDs = new TreeSet<Classification>(
				new MalletClassificationComparator()); // false negative

		for (int i = 0; i < trial.size(); i++) {
			Classification cl = trial.get(i);
			Instance instance = cl.getInstance();

			// String relationID = (String) instance.getName();
			// FeatureVector fv = (FeatureVector) instance.getData();
			Label trueLabel = instance.getLabeling().getBestLabel();
			Label classified_label = cl.getLabeling().getBestLabel();

			if (trueLabel.toString().equalsIgnoreCase(relationType)) {
				if (trueLabel.equals(classified_label)) { // true positive
					tpRelationIDs.add(cl);
				} else { // false negative
					fnRelationIDs.add(cl);
				}
			} else {
				if (trueLabel.equals(classified_label)) { // true negative
					tnRelationIDs.add(cl);
				} else {
					fpRelationIDs.add(cl);// false positive
				}
			}
		}

		PrintWriter tpout = null;
		PrintWriter fnout = null;
		PrintWriter tnout = null;
		PrintWriter fpout = null;
		try {
			try {
				tpout = new PrintWriter(rootDir + "/" + truePositiveFilename,
						fileEncoding);
				fnout = new PrintWriter(rootDir + "/" + falseNegativeFilename,
						fileEncoding);
				tnout = new PrintWriter(rootDir + "/" + trueNegativeFilename,
						fileEncoding);
				fpout = new PrintWriter(rootDir + "/" + falsePositiveFilename,
						fileEncoding);

				fnout.println("False Negative: " + fnRelationIDs.size());
				for (Classification cl : fnRelationIDs) {
					Instance instance = cl.getInstance();
					String relationID = (String) instance.getName();
					FeatureVector fv = (FeatureVector) instance.getData();
					Label trueLabel = instance.getLabeling().getBestLabel();
					Label classLabel = cl.getLabeling().getBestLabel();
					double probability = cl.getLabelVector().getBestValue();

					fnout.println(relationID + "\t" + trueLabel + "\t"
							+ classLabel + "\t" + probability + "\t"
							+ fv.toString(true));
				}
				fnout.flush();

				fpout.println("False Positive: " + fpRelationIDs.size());
				for (Classification cl : fpRelationIDs) {
					Instance instance = cl.getInstance();
					String relationID = (String) instance.getName();
					FeatureVector fv = (FeatureVector) instance.getData();
					Label trueLabel = instance.getLabeling().getBestLabel();
					Label classLabel = cl.getLabeling().getBestLabel();
					double probability = cl.getLabelVector().getBestValue();

					fpout.println(relationID + "\t" + trueLabel + "\t"
							+ classLabel + "\t" + probability + "\t"
							+ fv.toString(true));
				}
				fpout.flush();

				tpout.println("True Positive: " + tpRelationIDs.size());
				for (Classification cl : tpRelationIDs) {
					Instance instance = cl.getInstance();
					String relationID = (String) instance.getName();
					FeatureVector fv = (FeatureVector) instance.getData();
					Label trueLabel = instance.getLabeling().getBestLabel();
					Label classLabel = cl.getLabeling().getBestLabel();
					double probability = cl.getLabelVector().getBestValue();

					tpout.println(relationID + "\t" + trueLabel + "\t"
							+ classLabel + "\t" + probability + "\t"
							+ fv.toString(true));
				}
				tpout.flush();

				tnout.println("True Negative: " + tnRelationIDs.size());
				for (Classification cl : tnRelationIDs) {
					Instance instance = cl.getInstance();
					String relationID = (String) instance.getName();
					FeatureVector fv = (FeatureVector) instance.getData();
					Label trueLabel = instance.getLabeling().getBestLabel();
					Label classLabel = cl.getLabeling().getBestLabel();
					double probability = cl.getLabelVector().getBestValue();

					tnout.println(relationID + "\t" + trueLabel + "\t"
							+ classLabel + "\t" + probability + "\t"
							+ fv.toString(true));
				}
				tnout.flush();

			} finally {
				// int total = tpRelationIDs.size() + fpRelationIDs.size()
				// + tnRelationIDs.size() + fnRelationIDs.size();
				// System.out.println("\n\n\n total number of instance is: " +
				// total);

				fnout.close();
				fpout.close();
				tpout.close();
				tnout.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static void newMethodTrainingMaxEntClassifier(
			File local_file_directory, String oringinal_trainingSet_filename,
			File write_info_directory,
			boolean whether_loading_bllipparsing_file,
			boolean whether_loading_relation_annotation) {

		boolean if_using_filter_strategy_for_trainingSet = true;

		if (!local_file_directory.exists()
				|| !local_file_directory.isDirectory())
			throw new IllegalArgumentException(
					"The file folder does not exist!");

		GJHPubtatorDataset trainingSet = new GJHPubtatorDataset();
		trainingSet.loadDatasetFromLocalFiles(local_file_directory,
				oringinal_trainingSet_filename,
				whether_loading_bllipparsing_file,
				whether_loading_relation_annotation);

		Alphabet dataAlphabet_Cooccur = new Alphabet();
		LabelAlphabet targetAlphabet_Cooccur = new LabelAlphabet();
		Alphabet dataAlphabet_Uncooccur = new Alphabet();
		LabelAlphabet targetAlphabet_Uncooccur = new LabelAlphabet();

		FeatureSet featureSet_Cooccur = new FeatureSet(dataAlphabet_Cooccur,
				targetAlphabet_Cooccur,
				FeatrueConstructionMethod.Mention_Level_Cooccur);
		FeatureSet featureSet_Uncooccur = new FeatureSet(dataAlphabet_Uncooccur,
				targetAlphabet_Uncooccur,
				FeatrueConstructionMethod.Mention_Level_Uncooccur);

		InstanceList trainingInstances_Cooccur = new InstanceList(
				featureSet_Cooccur.getPipe());
		InstanceList trainingInstances_Uncooccur = new InstanceList(
				featureSet_Uncooccur.getPipe());

		if (if_using_filter_strategy_for_trainingSet) {

			List<Relation> trainRels = trainingSet
					.formNewRelaionsOfTheDataset();

			trainRels = RelationFilter4MentionLevel
					.filterTrainingSetRelationsByMeSH(trainRels,
							getMeshThesaurus());
			// trainRels =
			// filterTrainingSetRelationsByMeSH_v2(trainRels,
			// getMeshThesaurus());

			List<Relation> originalTrainRels_CooccurInOneSentence = new ArrayList<Relation>();

			List<Relation> originalTrainRels_UncooccurInOneSentence = new ArrayList<Relation>();
			for (Relation r : trainRels) {
				if (r.hasCooccurrenceInOneSentence()) {
					// System.out.println("there is one!");
					originalTrainRels_CooccurInOneSentence.add(r);
				} else {
					originalTrainRels_UncooccurInOneSentence.add(r);
				}
			}

			List<MentionPairAsInstanceSource> instanceSources = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTrainingCooccurrence(
							originalTrainRels_CooccurInOneSentence);
			// // balanced
			// instanceSources = balancedTrainSet_MentionLevel(instanceSources);
			for (int i = 0; i < instanceSources.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSources
						.get(i);

				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				trainingInstances_Cooccur.addThruPipe(instance);
			}

			List<MentionPairAsInstanceSource> instanceSourcesUncooccur = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTrainingUncooccurrence(
							originalTrainRels_UncooccurInOneSentence);
			// // balanced
			// instanceSourcesUncooccur =
			// balancedTrainSet_MentionLevel(instanceSourcesUncooccur);
			for (int i = 0; i < instanceSourcesUncooccur.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSourcesUncooccur
						.get(i);

				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				trainingInstances_Uncooccur.addThruPipe(instance);
			}
		} else {// not use filtering strategies

			// all training relations
			List<Relation> allTrainRels = trainingSet
					.formNewRelaionsOfTheDataset();

			List<Relation> originalTrainRels_CooccurInOneSentence = new ArrayList<Relation>();

			List<Relation> originalTrainRels_UncooccurInOneSentence = new ArrayList<Relation>();

			for (Relation r : allTrainRels) {
				if (r.hasCooccurrenceInOneSentence()) {
					// System.out.println("there is one!");
					List<Mention> cooccurChemMentions = new ArrayList<Mention>();
					List<Mention> cooccurDisMentions = new ArrayList<Mention>();
					r.getCoocurrenceMentionPairsInOneSentence(
							cooccurChemMentions, cooccurDisMentions);
					originalTrainRels_CooccurInOneSentence.add(r);

					for (int i = 0; i < cooccurChemMentions.size(); i++) {
						MentionPairAsInstanceSource menthonPairInstance = new MentionPairAsInstanceSource(
								cooccurChemMentions.get(i),
								cooccurDisMentions.get(i), r);

						Instance instance = new Instance(menthonPairInstance,
								null, null, menthonPairInstance);
						trainingInstances_Cooccur.addThruPipe(instance);
					}

				} else {
					originalTrainRels_UncooccurInOneSentence.add(r);
					List<Mention> unCooccurChemMentions = new ArrayList<Mention>(
							r.getChemicalMentions());
					List<Mention> unCooccurDisMentions = new ArrayList<Mention>(
							r.getDiseaseMentions());

					for (int i = 0; i < unCooccurChemMentions.size(); i++) {
						for (int j = 0; j < unCooccurDisMentions.size(); j++) {
							MentionPairAsInstanceSource menthonPairInstance = new MentionPairAsInstanceSource(
									unCooccurChemMentions.get(i),
									unCooccurDisMentions.get(j), r);

							Instance instance = new Instance(
									menthonPairInstance, null, null,
									menthonPairInstance);
							trainingInstances_Uncooccur.addThruPipe(instance);
						}
					}
				}
			}
		}

		writeInstanceFeatures(write_info_directory,
				oringinal_trainingSet_filename + ".cooccur.fv", "utf-8",
				trainingInstances_Cooccur);
		writeInstanceSource_MentionLevel_Cooccur(write_info_directory,
				oringinal_trainingSet_filename + ".cooccur.source", "utf-8",
				trainingInstances_Cooccur);
		writeInstanceFeatures(write_info_directory,
				oringinal_trainingSet_filename + ".uncooccur.fv", "utf-8",
				trainingInstances_Uncooccur);
		writeInstanceSource_MentionLevel_Uncooccur(write_info_directory,
				oringinal_trainingSet_filename + ".uncooccur.source", "utf-8",
				trainingInstances_Uncooccur);

		Classifier maxentclassifier_Cooccur = trainClassifier(
				trainingInstances_Cooccur);

		Classifier maxentclassifier_Uncooccur = trainClassifier(
				trainingInstances_Uncooccur);

		// output model
		MaxEntModel4MentionLevel model = new MaxEntModel4MentionLevel(
				dataAlphabet_Cooccur, targetAlphabet_Cooccur,
				maxentclassifier_Cooccur, dataAlphabet_Uncooccur,
				targetAlphabet_Uncooccur, maxentclassifier_Uncooccur);

		File model_directory = new File(
				write_info_directory.getPath() + "/" + "model");
		model.saveModel(model_directory);
	}

	public static void newMethodTestMaxEntClassifier(
			File loading_model_directory, File loading_file_directory,
			String original_dataset_name, File output_result_directory,
			boolean whether_loading_bllipparsing_file,
			boolean whether_loading_relation_annotation) throws IOException {
		boolean if_using_filter_strategy_for_testSet = true;

		MaxEntModel4MentionLevel model = new MaxEntModel4MentionLevel(
				loading_model_directory);

		Alphabet dataAlphabet_Cooccur = model.getDataAlphabet_Cooccur();
		LabelAlphabet targetAlphabet_Cooccur = model
				.getTargetAlphabet_Cooccur();
		Classifier maxentclassifier_Cooccur = model
				.getMaxentclassifier_Cooccur();

		Alphabet dataAlphabet_Uncooccur = model.getDataAlphabet_Uncooccur();
		LabelAlphabet targetAlphabet_Uncooccur = model
				.getTargetAlphabet_Uncooccur();
		Classifier maxentclassifier_Uncooccur = model
				.getMaxentclassifier_Uncooccur();

		FeatureSet featureSet_Cooccur = new FeatureSet(dataAlphabet_Cooccur,
				targetAlphabet_Cooccur,
				FeatrueConstructionMethod.Mention_Level_Cooccur);
		FeatureSet featureSet_Uncooccur = new FeatureSet(dataAlphabet_Uncooccur,
				targetAlphabet_Uncooccur,
				FeatrueConstructionMethod.Mention_Level_Uncooccur);

		GJHPubtatorDataset testingSet = new GJHPubtatorDataset();
		testingSet.loadDatasetFromLocalFiles(loading_file_directory,
				original_dataset_name, whether_loading_bllipparsing_file,
				whether_loading_relation_annotation);

		InstanceList testInstances_Cooccur = new InstanceList(
				featureSet_Cooccur.getPipe());

		InstanceList testInstances_Uncooccur = new InstanceList(
				featureSet_Uncooccur.getPipe());

		if (if_using_filter_strategy_for_testSet) {

			List<Relation> testRels = testingSet.formNewRelaionsOfTheDataset();

			testRels = RelationFilter4MentionLevel
					.filterTestSetRelationsByMeSH(testRels, getMeshThesaurus());

			List<Relation> originalTestingRels_CooccurInOneSentence = new ArrayList<Relation>();
			List<Relation> originalTestingRels_UncooccurInOneSentence = new ArrayList<Relation>();

			for (Relation r : testRels) {
				if (r.hasCooccurrenceInOneSentence()) {
					// System.out.println("there is one!");
					originalTestingRels_CooccurInOneSentence.add(r);
				} else {
					originalTestingRels_UncooccurInOneSentence.add(r);
				}
			}

			List<MentionPairAsInstanceSource> instanceSources = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTestingCooccurrence(
							originalTestingRels_CooccurInOneSentence);
			for (int i = 0; i < instanceSources.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSources
						.get(i);
				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				testInstances_Cooccur.addThruPipe(instance);
			}

			List<MentionPairAsInstanceSource> instanceSourcesUncooccur = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTestingUncooccurrence(
							originalTestingRels_UncooccurInOneSentence);
			for (int i = 0; i < instanceSourcesUncooccur.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSourcesUncooccur
						.get(i);

				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				testInstances_Uncooccur.addThruPipe(instance);
			}
		} else {
			List<Relation> testRels = testingSet.formNewRelaionsOfTheDataset();

			testRels = RelationFilter4MentionLevel
					.filterTestSetRelationsByMeSH(testRels, getMeshThesaurus());

			List<Relation> originalTestingRels_CooccurInOneSentence = new ArrayList<Relation>();
			List<Relation> originalTestingRels_UncooccurInOneSentence = new ArrayList<Relation>();

			for (Relation r : testRels) {
				if (r.hasCooccurrenceInOneSentence()) {
					// System.out.println("there is one!");
					originalTestingRels_CooccurInOneSentence.add(r);
					List<Mention> cooccurChemMentions = new ArrayList<Mention>();
					List<Mention> cooccurDisMentions = new ArrayList<Mention>();
					r.getCoocurrenceMentionPairsInOneSentence(
							cooccurChemMentions, cooccurDisMentions);

					assert (cooccurChemMentions.size() == cooccurDisMentions
							.size());

					for (int i = 0; i < cooccurChemMentions.size(); i++) {
						MentionPairAsInstanceSource menthonPairInstance = new MentionPairAsInstanceSource(
								cooccurChemMentions.get(i),
								cooccurDisMentions.get(i), r);

						Instance instance = new Instance(menthonPairInstance,
								null, null, menthonPairInstance);
						testInstances_Cooccur.addThruPipe(instance);
					}

				} else {
					originalTestingRels_UncooccurInOneSentence.add(r);
					List<Mention> unCooccurChemMentions = new ArrayList<Mention>(
							r.getChemicalMentions());
					List<Mention> unCooccurDisMentions = new ArrayList<Mention>(
							r.getDiseaseMentions());

					for (int i = 0; i < unCooccurChemMentions.size(); i++) {
						for (int j = 0; j < unCooccurDisMentions.size(); j++) {
							MentionPairAsInstanceSource menthonPairInstance = new MentionPairAsInstanceSource(
									unCooccurChemMentions.get(i),
									unCooccurDisMentions.get(j), r);

							Instance instance = new Instance(
									menthonPairInstance, null, null,
									menthonPairInstance);
							testInstances_Uncooccur.addThruPipe(instance);
						}
					}
				}
			}
		}

		writeInstanceFeatures(output_result_directory,
				original_dataset_name + ".cooccur.fv", "utf-8",
				testInstances_Cooccur);
		writeInstanceSource_MentionLevel_Cooccur(output_result_directory,
				original_dataset_name + ".cooccur.source", "utf-8",
				testInstances_Cooccur);
		writeInstanceFeatures(output_result_directory,
				original_dataset_name + ".uncooccur.fv", "utf-8",
				testInstances_Uncooccur);
		writeInstanceSource_MentionLevel_Uncooccur(output_result_directory,
				original_dataset_name + ".uncooccur.source", "utf-8",
				testInstances_Uncooccur);

		Trial mentionLevelTrial_cooccur = new Trial(maxentclassifier_Cooccur,
				testInstances_Cooccur);
		Trial mentionLevelTrial_uncooccur = new Trial(
				maxentclassifier_Uncooccur, testInstances_Uncooccur);

		System.out.println("\n**Mention Level Cooccurrence Solution: ");
		evaluate_MentionLevel(mentionLevelTrial_cooccur,
				output_result_directory, "cooccurrence");
		System.out.println("\n**Mention Level Uncooccurrence Solution: ");
		evaluate_MentionLevel(mentionLevelTrial_uncooccur,
				output_result_directory, "unCooccurrence");
		writeCIDRecognitionResult_MentionLevel(output_result_directory,
				mentionLevelTrial_cooccur, mentionLevelTrial_uncooccur);
	}

	public static void tryAnotherMethod_Processing_MentionLevel(
			File training_file_directory, String trainingSet_name,
			File write_info_directory,
			boolean whether_loading_traingingSet_relation,
			File test_file_directory, String testSet_name,
			File output_result_directory,
			boolean whether_loading_testSet_relation_annotation,
			File gold_file_for_evaluation) throws IOException {

		newMethodTrainingMaxEntClassifier(training_file_directory,
				trainingSet_name, write_info_directory, true,
				whether_loading_traingingSet_relation);

		// get the training model directory
		File model_directory = new File(write_info_directory + "/" + "model");
		System.out.println("Model directory: " + model_directory.getPath());

		newMethodTestMaxEntClassifier(model_directory, test_file_directory,
				testSet_name, output_result_directory, true,
				whether_loading_testSet_relation_annotation);

		bc5CIDEvaluation("relation", "CID", gold_file_for_evaluation, new File(
				output_result_directory.getPath() + "/" + "CID_result.txt"));
		// bc5CIDEvaluation("relation", "CID",
		// "Corpus/CDR_TestSet.PubTator.txt",
		// rootDir + "/" + "CID_result.txt");
	}

	public static void bc5CIDEvaluation(String evalName, String evalType,
			File gold_annotation_file, File our_result_file) {
		if (!gold_annotation_file.exists() || !gold_annotation_file.isFile())
			throw new IllegalArgumentException(
					"The gold annotation file does not exist!");

		if (!our_result_file.exists() || !our_result_file.isFile())
			throw new IllegalArgumentException(
					"The input result file does not exist!");

		System.out.println("\n**** Customized Method is Ending ****\n");
		System.out.println("\n**** BC5 Evaluation Starting... ****");
		// String evalArgs[] = new String[] { "relation", "CID", "PubTator",
		// goldFilePath, resultFilePath };
		String evalArgs[] = new String[] { evalName, evalType, "PubTator",
				gold_annotation_file.getPath(), our_result_file.getPath() };
		ncbi.bc5cdr_eval.Evaluate.main(evalArgs);
	}

	// predicting on the test set
	public static void predictingOnDemos(File model_directory,
			File loading_file_directory, String original_dataset_name,
			File output_result_directory) throws IOException {
		boolean isTestingDatasetUseFilterStrategy = true;

		MaxEntModel4MentionLevel model = new MaxEntModel4MentionLevel(
				model_directory);

		Alphabet dataAlphabet_Cooccur = model.getDataAlphabet_Cooccur();
		LabelAlphabet targetAlphabet_Cooccur = model
				.getTargetAlphabet_Cooccur();
		Classifier maxentclassifier_Cooccur = model
				.getMaxentclassifier_Cooccur();

		Alphabet dataAlphabet_Uncooccur = model.getDataAlphabet_Uncooccur();
		LabelAlphabet targetAlphabet_Uncooccur = model
				.getTargetAlphabet_Uncooccur();
		Classifier maxentclassifier_Uncooccur = model
				.getMaxentclassifier_Uncooccur();

		FeatureSet featureSet_Cooccur = new FeatureSet(dataAlphabet_Cooccur,
				targetAlphabet_Cooccur,
				FeatrueConstructionMethod.Mention_Level_Cooccur);
		FeatureSet featureSet_Uncooccur = new FeatureSet(dataAlphabet_Uncooccur,
				targetAlphabet_Uncooccur,
				FeatrueConstructionMethod.Mention_Level_Uncooccur);

		GJHPubtatorDataset testingSet = new GJHPubtatorDataset();
		testingSet.loadDatasetFromLocalFiles(loading_file_directory,
				original_dataset_name, true, true);

		InstanceList testInstances_Cooccur = new InstanceList(
				featureSet_Cooccur.getPipe());

		InstanceList testInstances_Uncooccur = new InstanceList(
				featureSet_Uncooccur.getPipe());

		if (isTestingDatasetUseFilterStrategy) {

			List<Relation> testRels = testingSet.formNewRelaionsOfTheDataset();

			testRels = RelationFilter4MentionLevel
					.filterTestSetRelationsByMeSH(testRels, getMeshThesaurus());

			List<Relation> originalTestingRels_CooccurInOneSentence = new ArrayList<Relation>();
			List<Relation> originalTestingRels_UncooccurInOneSentence = new ArrayList<Relation>();

			for (Relation r : testRels) {
				if (r.hasCooccurrenceInOneSentence()) {
					// System.out.println("there is one!");
					originalTestingRels_CooccurInOneSentence.add(r);
				} else {
					originalTestingRels_UncooccurInOneSentence.add(r);
				}
			}

			List<MentionPairAsInstanceSource> instanceSources = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTestingCooccurrence(
							originalTestingRels_CooccurInOneSentence);
			for (int i = 0; i < instanceSources.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSources
						.get(i);
				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				testInstances_Cooccur.addThruPipe(instance);
			}

			List<MentionPairAsInstanceSource> instanceSourcesUncooccur = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTestingUncooccurrence(
							originalTestingRels_UncooccurInOneSentence);
			for (int i = 0; i < instanceSourcesUncooccur.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSourcesUncooccur
						.get(i);

				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				testInstances_Uncooccur.addThruPipe(instance);
			}
		} else {
			List<Relation> testRels = testingSet.formNewRelaionsOfTheDataset();

			testRels = RelationFilter4MentionLevel
					.filterTestSetRelationsByMeSH(testRels, getMeshThesaurus());

			List<Relation> originalTestingRels_CooccurInOneSentence = new ArrayList<Relation>();
			List<Relation> originalTestingRels_UncooccurInOneSentence = new ArrayList<Relation>();

			for (Relation r : testRels) {
				if (r.hasCooccurrenceInOneSentence()) {
					// System.out.println("there is one!");
					originalTestingRels_CooccurInOneSentence.add(r);
					List<Mention> cooccurChemMentions = new ArrayList<Mention>();
					List<Mention> cooccurDisMentions = new ArrayList<Mention>();
					r.getCoocurrenceMentionPairsInOneSentence(
							cooccurChemMentions, cooccurDisMentions);

					assert (cooccurChemMentions.size() == cooccurDisMentions
							.size());

					for (int i = 0; i < cooccurChemMentions.size(); i++) {
						MentionPairAsInstanceSource menthonPairInstance = new MentionPairAsInstanceSource(
								cooccurChemMentions.get(i),
								cooccurDisMentions.get(i), r);

						Instance instance = new Instance(menthonPairInstance,
								null, null, menthonPairInstance);
						testInstances_Cooccur.addThruPipe(instance);
					}

				} else {
					originalTestingRels_UncooccurInOneSentence.add(r);
					List<Mention> unCooccurChemMentions = new ArrayList<Mention>(
							r.getChemicalMentions());
					List<Mention> unCooccurDisMentions = new ArrayList<Mention>(
							r.getDiseaseMentions());

					for (int i = 0; i < unCooccurChemMentions.size(); i++) {
						for (int j = 0; j < unCooccurDisMentions.size(); j++) {
							MentionPairAsInstanceSource menthonPairInstance = new MentionPairAsInstanceSource(
									unCooccurChemMentions.get(i),
									unCooccurDisMentions.get(j), r);

							Instance instance = new Instance(
									menthonPairInstance, null, null,
									menthonPairInstance);
							testInstances_Uncooccur.addThruPipe(instance);
						}
					}
				}
			}
		}

		writeInstanceFeatures(output_result_directory,
				original_dataset_name + ".cooccur.fv", "utf-8",
				testInstances_Cooccur);
		writeInstanceSource_MentionLevel_Cooccur(output_result_directory,
				original_dataset_name + ".cooccur.source", "utf-8",
				testInstances_Cooccur);
		writeInstanceFeatures(output_result_directory,
				original_dataset_name + ".uncooccur.fv", "utf-8",
				testInstances_Uncooccur);
		writeInstanceSource_MentionLevel_Uncooccur(output_result_directory,
				original_dataset_name + ".uncooccur.source", "utf-8",
				testInstances_Uncooccur);

		Trial mentionLevelTrial_cooccur = new Trial(maxentclassifier_Cooccur,
				testInstances_Cooccur);
		Trial mentionLevelTrial_uncooccur = new Trial(
				maxentclassifier_Uncooccur, testInstances_Uncooccur);
		writeCIDRecognitionResult_MentionLevel(output_result_directory,
				mentionLevelTrial_cooccur, mentionLevelTrial_uncooccur);
	}

	// predicting on files in a specific folder
	public static void predictingFilesInFolder() throws IOException {
		File model_directory = new File(
				"trained_models/model_trained_by_training_and_development_set");

		File before_post_processing = new File("temp/before_post_processing");
		if (!before_post_processing.exists()
				|| !before_post_processing.isDirectory())
			before_post_processing.mkdir();

		File outputPath = new File("output");
		if (!outputPath.exists() || !outputPath.isDirectory())
			outputPath.mkdir();
		else
			FileUtils.cleanDirectory(outputPath);

		MaxEntModel4MentionLevel model = new MaxEntModel4MentionLevel(
				model_directory);

		Alphabet dataAlphabet_Cooccur = model.getDataAlphabet_Cooccur();
		LabelAlphabet targetAlphabet_Cooccur = model
				.getTargetAlphabet_Cooccur();
		Classifier maxentclassifier_Cooccur = model
				.getMaxentclassifier_Cooccur();

		Alphabet dataAlphabet_Uncooccur = model.getDataAlphabet_Uncooccur();
		LabelAlphabet targetAlphabet_Uncooccur = model
				.getTargetAlphabet_Uncooccur();
		Classifier maxentclassifier_Uncooccur = model
				.getMaxentclassifier_Uncooccur();

		FeatureSet featureSet_Cooccur = new FeatureSet(dataAlphabet_Cooccur,
				targetAlphabet_Cooccur,
				FeatrueConstructionMethod.Mention_Level_Cooccur);
		FeatureSet featureSet_Uncooccur = new FeatureSet(dataAlphabet_Uncooccur,
				targetAlphabet_Uncooccur,
				FeatrueConstructionMethod.Mention_Level_Uncooccur);

		File dir = new File("input");
		if (!dir.exists() || !dir.isDirectory())
			throw new IllegalStateException("The input directory is missing!");
		
		String files[] = dir.list();
		System.out.println("*** There are " + files.length
				+ " files in \"input\" folder for relation extraction! ***");
		for (String filename : files) {
			File aFile = new File(dir.getPath() + "/" + filename);

			GJHPubtatorDataset testingSet = new GJHPubtatorDataset();
			testingSet.loadFromOnePubtatorFile(aFile, "utf-8");
			InstanceList testInstances_Cooccur = new InstanceList(
					featureSet_Cooccur.getPipe());
			InstanceList testInstances_Uncooccur = new InstanceList(
					featureSet_Uncooccur.getPipe());

			List<Relation> testRels = testingSet.formNewRelaionsOfTheDataset();
			testRels = RelationFilter4MentionLevel
					.filterTestSetRelationsByMeSH(testRels, getMeshThesaurus());

			List<Relation> originalTestingRels_CooccurInOneSentence = new ArrayList<Relation>();
			List<Relation> originalTestingRels_UncooccurInOneSentence = new ArrayList<Relation>();

			for (Relation r : testRels) {
				if (r.hasCooccurrenceInOneSentence()) {
					// System.out.println("there is one!");
					originalTestingRels_CooccurInOneSentence.add(r);
				} else {
					originalTestingRels_UncooccurInOneSentence.add(r);
				}
			}

			// cooccurrence
			List<MentionPairAsInstanceSource> instanceSources = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTestingCooccurrence(
							originalTestingRels_CooccurInOneSentence);
			for (int i = 0; i < instanceSources.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSources
						.get(i);
				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				testInstances_Cooccur.addThruPipe(instance);
			}

			// unCooccurrence
			List<MentionPairAsInstanceSource> instanceSourcesUncooccur = InstanceFilter4MentionLevel
					.filterMentionLevelInstancesOfTestingUncooccurrence(
							originalTestingRels_UncooccurInOneSentence);
			for (int i = 0; i < instanceSourcesUncooccur.size(); i++) {
				MentionPairAsInstanceSource instanceSource = instanceSourcesUncooccur
						.get(i);

				Instance instance = new Instance(instanceSource, null, null,
						instanceSource);
				testInstances_Uncooccur.addThruPipe(instance);
			}

			FileUtils.cleanDirectory(before_post_processing);

			// writeInstanceFeatures(outputPath.getPath(),
			// filename + ".cooccur.fv", "utf-8", testInstances_Cooccur);
			// writeInstanceSource_MentionLevel_Cooccur(outputPath.getPath(),
			// filename + ".cooccur.source", "utf-8",
			// testInstances_Cooccur);
			// writeInstanceFeatures(outputPath.getPath(),
			// filename + ".uncooccur.fv", "utf-8",
			// testInstances_Uncooccur);
			// writeInstanceSource_MentionLevel_Uncooccur(outputPath.getPath(),
			// filename + ".uncooccur.source", "utf-8",
			// testInstances_Uncooccur);

			Trial mentionLevelTrial_cooccur = new Trial(
					maxentclassifier_Cooccur, testInstances_Cooccur);
			Trial mentionLevelTrial_uncooccur = new Trial(
					maxentclassifier_Uncooccur, testInstances_Uncooccur);

			writeCIDRecognitionResult_MentionLevel(before_post_processing,
					mentionLevelTrial_cooccur, mentionLevelTrial_uncooccur);

			postProcessing(before_post_processing.getPath() + "/CID_result.txt",
					before_post_processing.getPath()
							+ "/CID_result_after_post_processing.txt",
					getMeshThesaurus());

			List<Abstract> abs = testingSet.getAbstracts();
			Map<String, Abstract> map = new HashMap<String, Abstract>();
			for (Abstract a : abs) {
				map.put(a.getDocumentID(), a);
			}
			Scanner in = null;
			try {
				try {
					in = new Scanner(
							new File(before_post_processing.getPath()
									+ "/CID_result_after_post_processing.txt"),
							"utf-8");
					while (in.hasNextLine()) {
						String line = in.nextLine();
						String[] ss = line.split("\\t");

						String documentID = ss[0];
						String relationType = ss[1];
						String chemicalConceptID = ss[2];
						String diseaseConceptID = ss[3];
						Double probability = Double.valueOf(ss[4]);
						Abstract ab = map.get(ss[0]);
						Relation r = new Relation(documentID, ab, relationType,
								chemicalConceptID, diseaseConceptID,
								probability);
						ab.addRelation(r);

					}
				} finally {
					in.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			PrintWriter final_output = null;
			try {
				try {
					final_output = new PrintWriter(
							outputPath.getPath() + "/" + filename, "utf-8");

					for (Abstract a : abs) {
						final_output.println(a.toString());

					}
					final_output.flush();
				} finally {
					final_output.close();
				}
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args)
			throws FileNotFoundException, IOException, InterruptedException {

		/************ +++main function+++ ************/

		predictingFilesInFolder();

		/************ ---main function--- ************/

		System.out.println("\n**** End ****\n");

	}

}