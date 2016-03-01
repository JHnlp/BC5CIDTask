package gjh.bc5.dataset;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.NotImplementedException;

import dragon.nlp.Word;
import dragon.nlp.tool.HeppleTagger;
import dragon.nlp.tool.Lemmatiser;
import dragon.nlp.tool.MedPostTagger;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import dragon.util.EnvVariable;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.sequences.PlainTextDocumentReaderAndWriter.OutputStyle;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.EntityType;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Sentence;
import gjh.bc5.utils.TinyUtils;
import gjh.bc5.utils.Token;

/**
 * 
 * @author GJH
 * 
 */
public class GJHPubtatorDataset extends Dataset {

	private List<Abstract> abstracts;

	private SentenceBreakerWithStanford sentence_breaker;// sentence breaker
															// using stanford
															// tools with post
															// processing
	private Tagger dragonTool_pos_tagger;// from dragon tools library
	private Lemmatiser dragonTool_lemmatiser;
	private MaxentTagger stanford_maxent_POS_tagger;

	private static final String DRAGON_TOOL_POS_TAGGER_QUALIFIED_NAME = "dragon.nlp.tool.HeppleTagger";
	private static final String DRAGON_TOOL_POS_TAGGER_DATA_DIRECTORY = "nlpdata/tagger";
	private static final String DRAGON_TOOL_LEMMATISER_QUALIFIED_NAME = "dragon.nlp.tool.lemmatiser.EngLemmatiser";
	private static final String DRAGON_TOOL_LEMMATISER_DATA_DIRECTORY = "nlpdata/lemmatiser";

	// private LexicalizedParser lp;
	private static final String STANFORD_PARSER_MODEL_NAME = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
	// private static final String STANFORD_PARSER_MODEL_NAME =
	// "edu/stanford/nlp/models/lexparser/englishFactored.ser.gz";
	private static final String STANFORD_POS_CONFIG_STRING = "-model stanford_pos_model/english-left3words-distsim.tagger -tagSeparator @@ -outputFormatOptions lemmatize -sentenceDelimiter newline -tokenize false";

	public GJHPubtatorDataset() {
		abstracts = new ArrayList<Abstract>();
		// SentenceBreakerWithStanford stanSeger;

		sentence_breaker = new SentenceBreakerWithStanford();

		setTokenizer(new SimpleTokenizer());

		setPosTagger(DRAGON_TOOL_POS_TAGGER_QUALIFIED_NAME,
				DRAGON_TOOL_POS_TAGGER_DATA_DIRECTORY);

		setLemmatiser(DRAGON_TOOL_LEMMATISER_QUALIFIED_NAME,
				DRAGON_TOOL_LEMMATISER_DATA_DIRECTORY);

		stanford_maxent_POS_tagger = getStanfordPOSTagger(
				STANFORD_POS_CONFIG_STRING);
		// lp = null;
	}

	public List<Abstract> getAbstracts() {
		return abstracts;
	}

	public void setAbstracts(List<Abstract> abstracts) {
		this.abstracts = abstracts;
	}

	public void addAbstract(Abstract ab) {
		this.abstracts.add(ab);
	}

	private static boolean isPunctuation(char ch) {
		return ("`~!@#$%^&*()-–=_+[]\\{}|;':\",./<>?".indexOf(ch) != -1);
	}

	public static boolean isPunctuation(String str) {
		if (str.length() == 1) {
			char ch = str.charAt(0);
			return isPunctuation(ch);
		} else {
			return false;
		}
	}

	public void setPosTagger(String posTaggerName,
			String posTaggerDataDirectory) {
		EnvVariable.setDragonHome(".");
		EnvVariable.setCharSet("US-ASCII");

		if (posTaggerName == null) // "dragon.nlp.tool.HeppleTagger"
			throw new IllegalArgumentException("Must specify POS Tagger!");
		if (posTaggerDataDirectory == null) // "nlpdata/tagger"
			throw new IllegalArgumentException(
					"Must specify data directory for POS Tagger!");

		if (posTaggerName.equals(HeppleTagger.class.getName()))
			dragonTool_pos_tagger = new HeppleTagger(posTaggerDataDirectory);
		else if (posTaggerName.equals(MedPostTagger.class.getName()))
			dragonTool_pos_tagger = new MedPostTagger(posTaggerDataDirectory);
		else
			throw new IllegalArgumentException(
					"Unknown POS Tagger type: " + posTaggerName);
	}

	public void setLemmatiser(String lemmatiserName,
			String lemmatiserDataDirectory) {
		EnvVariable.setDragonHome(".");
		EnvVariable.setCharSet("US-ASCII");

		if (lemmatiserName == null) // "dragon.nlp.tool.lemmatiser.EngLemmatiser"
			throw new IllegalArgumentException("Must specify lemmatiser name!");
		if (lemmatiserDataDirectory == null)
			throw new IllegalArgumentException(
					"Must specify data directory for lemmatiser!");

		if (lemmatiserName.equals(EngLemmatiser.class.getName()))
			dragonTool_lemmatiser = new EngLemmatiser(lemmatiserDataDirectory,
					false, true);
		else
			throw new IllegalArgumentException(
					"Unknown lemmatiser type: " + lemmatiserName);
	}

	private void writeDataset(File output_folder, String dataset_name) {
		if (!output_folder.exists())
			output_folder.mkdirs();

		String path_prefix = output_folder.getPath() + "/";

		PrintWriter outAbstracts = null;
		PrintWriter outSentences = null;
		PrintWriter outMentions = null;
		PrintWriter outTokens = null;
		PrintWriter outRelations = null;
		PrintWriter outTokenizedSentences = null;

		PrintWriter outSyntacticParsingResults = null;
		PrintWriter outgraphDependencies = null;

		PrintWriter outWordsStatistics = null;
		PrintWriter outMentionConcealedSentences = null;
		PrintWriter outMentionConcealedSentencesForMccloskyParser = null;
		PrintWriter outMentionConcealedSentencesForGDep = null;
		try {
			try {
				// System.out.println(folderPrefix);
				// System.out.println(folderPrefix + abstractFilename);
				// System.out.println(folderPrefix + sentenceFilename);
				// System.out.println(folderPrefix + mentionFilename);
				// System.out.println(folderPrefix + tokenFilename);

				outAbstracts = new PrintWriter(
						path_prefix + dataset_name + ".abstract", "utf-8");
				outSentences = new PrintWriter(
						path_prefix + dataset_name + ".sentence", "utf-8");
				outMentions = new PrintWriter(
						path_prefix + dataset_name + ".mention", "utf-8");
				outTokens = new PrintWriter(
						path_prefix + dataset_name + ".token", "utf-8");
				outRelations = new PrintWriter(
						path_prefix + dataset_name + ".relation", "utf-8");
				outTokenizedSentences = new PrintWriter(
						path_prefix + dataset_name + ".tokenizedSentence",
						"utf-8");
				outSyntacticParsingResults = new PrintWriter(
						path_prefix + dataset_name + ".syntacticParsing",
						"utf-8");
				outgraphDependencies = new PrintWriter(
						path_prefix + dataset_name + ".dependency", "utf-8");
				outWordsStatistics = new PrintWriter(
						path_prefix + dataset_name + ".wordsStatistic",
						"utf-8");

				String mentionConcealedSentencesFilename = dataset_name
						+ ".mentionConcealedSentence";
				outMentionConcealedSentences = new PrintWriter(
						path_prefix + mentionConcealedSentencesFilename,
						"utf-8");
				outMentionConcealedSentencesForMccloskyParser = new PrintWriter(
						path_prefix
								+ mentionConcealedSentencesFilename.substring(0,
										mentionConcealedSentencesFilename
												.lastIndexOf('.'))
								+ ".SentenceForMcclosky",
						"utf-8");
				outMentionConcealedSentencesForGDep = new PrintWriter(
						path_prefix
								+ mentionConcealedSentencesFilename.substring(0,
										mentionConcealedSentencesFilename
												.lastIndexOf('.'))
								+ ".SentenceForGDep",
						"utf-8");

				List<Abstract> abs = this.getAbstracts();
				Collections.sort(abs);
				for (Abstract a : abs) {
					outAbstracts.println(a.getDocumentID() + "\t"
							+ a.getTitleText() + "\t" + a.getAbstractText());

					List<Sentence> sentences = a.getSentences();
					for (Sentence s : sentences) {
						outSentences.println(s.getDocumentID() + "\t"
								+ s.getSentenceIndex() + "\t"
								+ s.getSentStartOffset() + "\t" + s.getText());

						outMentionConcealedSentences.println(
								s.getDocumentID() + "\t" + s.getSentenceIndex()
										+ "\t" + s.getSentStartOffset() + "\t"
										+ s.getMentionConcealedText());

						outMentionConcealedSentencesForMccloskyParser.println(
								"<s> " + s.getMentionConcealedText() + " </s>");

						outMentionConcealedSentencesForGDep
								.println(s.getMentionConcealedText());

						// mentions
						List<Mention> mentions = s.getMentions();
						for (Mention m : mentions) {
							outMentions.println(m.getDocumentID() + "\t"
									+ m.getSentenceIndex() + "\t" + m.getText()
									+ "\t" + m.getEntityType().getTypeName()
									+ "\t" + m.getStartOffsetInDocument() + "\t"
									+ m.getEndOffsetInDocument() + "\t"
									+ m.getStartOffsetInSentence(s) + "\t"
									+ m.getEndOffsetInSentence(s) + "\t"
									+ m.getConceptID() + "\t"
									+ m.getNickyName());
						}

						// tokens
						List<Token> tokens = s.getTokens();
						String tokenizedSentenceText = "";
						for (int i = 0; i < tokens.size(); i++) {
							Token t = tokens.get(i);
							if (i == tokens.size() - 1) {
								tokenizedSentenceText += t.getText();
							} else {
								tokenizedSentenceText += t.getText() + " ";
							}
							outTokens.println(t.getDocumentID() + "\t"
									+ t.getSentenceIndex() + "\t" + t.getText()
									+ "\t" + t.getStartOffInSentence() + "\t"
									+ t.getEndOffInSentence() + "\t"
									+ t.getPos() + "\t" + t.getLemma() + "\t"
									+ t.getStanfordPOS());
						}

						outTokenizedSentences.println(
								s.getDocumentID() + "\t" + s.getSentenceIndex()
										+ "\t" + s.getSentStartOffset() + "\t"
										+ tokenizedSentenceText);
						outSyntacticParsingResults.println(
								s.getDocumentID() + "\t" + s.getSentenceIndex()
										+ "\t" + s.getSyntacticParsing());

						outgraphDependencies.println(s.getDocumentID() + "\t"
								+ s.getSentenceIndex() + "\t" + s.getText());

						Tree t = s.getTreeFromSyntacticParsing();
						if (t != null)
							// outgraphDependencies.println(graph.toList());
							outgraphDependencies.println(s
									.getTreePresentationString(
											"typedDependenciesCollapsed")
									.trim());
						else
							outgraphDependencies.println();
					}

					// relations
					List<Relation> relations = a.getRelations();
					for (Relation r : relations) {
						outRelations.println(
								r.getDocumentID() + "\t" + r.getRelationType()
										+ "\t" + r.getChemicalConceptID() + "\t"
										+ r.getDiseaseConceptID());
					}
				}

				Set<Token> allTokens = this.getTokens();
				Map<String, Integer> words = new HashMap<String, Integer>();
				for (Token t : allTokens) {
					String word = t.getText().toLowerCase();
					if (words.containsKey(word)) {
						int freq = words.get(word);
						words.put(word, freq + 1);
					} else {
						words.put(word, 1);
					}
				}
				// desending order by map value
				List<Map.Entry<String, Integer>> desendingOrder_list = TinyUtils
						.getDesendingOrderByMapValue_Integer(words);
				for (int j = 0; j < desendingOrder_list.size(); j++)
					outWordsStatistics
							.println(desendingOrder_list.get(j).getKey() + "\t"
									+ desendingOrder_list.get(j).getValue());

				outTokenizedSentences.flush();
				outSyntacticParsingResults.flush();
				outgraphDependencies.flush();
				outMentions.flush();
				outTokens.flush();
				outMentionConcealedSentences.flush();
				outMentionConcealedSentencesForMccloskyParser.flush();
				outMentionConcealedSentencesForGDep.flush();
				outSentences.flush();
				outRelations.flush();
				outAbstracts.flush();
				outWordsStatistics.flush();
			} finally {
				outTokenizedSentences.close();
				outSyntacticParsingResults.close();
				outgraphDependencies.close();
				outTokens.close();
				outMentions.close();
				outMentionConcealedSentences.close();
				outMentionConcealedSentencesForMccloskyParser.close();
				outMentionConcealedSentencesForGDep.close();
				outSentences.close();
				outRelations.close();
				outAbstracts.close();
				outWordsStatistics.close();
			}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	// mapping to the HeppleTagger
	public String getMappingNameFromHeppleTagger(int posIndex) {
		if (posIndex == 0)
			return "POS_PUNCTUATION";
		else if (posIndex == 1)
			return "POS_NOUN";
		else if (posIndex == 2)
			return "POS_VERB";
		else if (posIndex == 3)
			return "POS_ADJECTIVE";
		else if (posIndex == 4)
			return "POS_ADVERB";
		else if (posIndex == 5)
			return "POS_IN";
		else if (posIndex == 6)
			return "POS_PRONOUN";
		else if (posIndex == 7)
			return "POS_DT";
		else if (posIndex == 8)
			return "POS_CC";
		else if (posIndex == 9)
			return "POS_NUM";
		else
			throw new IllegalArgumentException("posIndex: " + posIndex
					+ " cannot be found in HeppleTagger definition!");
	}

	public MaxentTagger getStanfordPOSTagger(String configStr) {
		// String configStr =
		// "-model stanford_pos_model/english-left3words-distsim.tagger
		// -textFile gjh.txt -tagSeparator @@ -outputFormatOptions lemmatize
		// -sentenceDelimiter newline -tokenize false -outputFile gjh.pos.txt";
		TaggerConfig config = new TaggerConfig(configStr.split("\\s"));
		MaxentTagger maxentTagger = new MaxentTagger(config);

		return maxentTagger;
	}

	public List<String> getStanfordPOSResults(final MaxentTagger maxentTagger,
			final Sentence sentence) throws IOException {
		List<Token> tokens = sentence.getTokens();
		String tokenizedSentence = "";
		for (int i = 0; i < tokens.size(); i++) {
			if (i == tokens.size() - 1) {
				tokenizedSentence += tokens.get(i).getText();
			} else {
				tokenizedSentence += tokens.get(i).getText() + " ";
			}
		}
		// System.out.println(sentence_with_split);

		BufferedReader br = new BufferedReader(
				new StringReader(tokenizedSentence));
		StringWriter sw = new StringWriter();
		BufferedWriter bw = new BufferedWriter(sw);
		maxentTagger.runTaggerStdin(br, bw, OutputStyle.SLASH_TAGS);
		bw.flush();
		StringBuffer sb = sw.getBuffer();
		String posStringResults = sb.toString();
		String[] posResults = posStringResults.split("\\s");

		List<String> partOfSpeechs = new ArrayList<String>();
		for (int i = 0; i < posResults.length; i++) {
			String tmp = posResults[i];
			String[] text_With_POS = tmp.split("@@");
			String pos = text_With_POS[1];

			partOfSpeechs.add(pos);
		}

		return Collections.unmodifiableList(partOfSpeechs);
	}

	public LexicalizedParser getStanfordParser(String modelName) {
		// model is (e.g. edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz)
		LexicalizedParser lp = LexicalizedParser.loadModel(modelName);

		return lp;
	}

	// stanford default tokenizer
	public List<CoreLabel> getTokenizationResultsByStanfordTokenizer(
			String text) {
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(
				new StringReader(text), new CoreLabelTokenFactory(), "");

		List<CoreLabel> words = new ArrayList<CoreLabel>();
		while (ptbt.hasNext()) {
			CoreLabel label = ptbt.next();
			// System.out.println(label);

			words.add(label);
		}
		return words;
	}

	public edu.stanford.nlp.trees.Tree getParseResult(LexicalizedParser lp,
			String text) {
		List<CoreLabel> rawWords = getTokenizationResultsByStanfordTokenizer(
				text);
		Tree parseTree = lp.apply(rawWords);

		// parse.pennPrint();
		// System.out.println(parse.getClass());

		return parseTree;
	}

	public List<TypedDependency> getDependencyResults(LexicalizedParser lp,
			Tree parseTree) {
		TreebankLanguagePack tlp = lp.treebankLanguagePack(); // PennTreebankLanguagePack
		// for English
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parseTree);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

		// for (TypedDependency td : tdl)
		// System.out.println(td);

		return tdl;
	}

	public void loadDataset(File pubtator_file, String encoding,
			File output_folder, boolean ifUseStanfordCoreNLPParser) {
		if (!pubtator_file.exists() || !pubtator_file.isFile())
			throw new IllegalArgumentException("The file does not exist!");

		BufferedReader rd = null;
		ArrayDeque<String> abstractQueue = new ArrayDeque<String>();
		try {
			try {
				rd = new BufferedReader(new InputStreamReader(
						new FileInputStream(pubtator_file.getPath()),
						encoding));
				String line = rd.readLine();

				while (line != null) {
					if (line.isEmpty()) {
						String docid = null;
						String title = null;
						String abText = null;
						List<Sentence> sentences = new ArrayList<Sentence>();
						List<Mention> mentions = new ArrayList<Mention>();
						List<Relation> relations = new ArrayList<Relation>();

						while (!abstractQueue.isEmpty()) {
							String currentLine = abstractQueue.poll();
							// System.out.println(tmp);

							// title
							if (currentLine.indexOf("|t|") != -1) {
								String[] ss = currentLine.split("\\|t\\|");
								docid = ss[0];
								title = ss[1];
								continue;// finish one line
							}

							// abstract
							if (currentLine.indexOf("|a|") != -1) {
								String[] ss = currentLine.split("\\|a\\|");
								if (!ss[0].equals(docid)) {
									throw new IllegalStateException(
											"The docid between title and abstract does not match!");
								}
								abText = ss[1];

								// ssplit
								sentence_breaker.setText(title, abText);
								sentence_breaker
										.segementSentencesWithPostProcessing();
								List<String> sentenceTexts = sentence_breaker
										.getSentences();
								List<Integer> sentOffs = sentence_breaker
										.getSentenceStartOffset();
								assert (sentenceTexts.size() == sentOffs
										.size());

								for (int i = 0; i < sentOffs.size(); i++) {
									Sentence s = new Sentence(docid, i,
											sentenceTexts.get(i),
											sentOffs.get(i));

									// addToken
									tokenizer.tokenize(s);
									// System.out.println(s);
									// System.out.println(s.getTokens().size());

									// pos & lemma
									dragon.nlp.Sentence dragonSentence = null;
									List<Token> tokens = s.getTokens();
									int size = tokens.size();
									List<String> stanfordPOSs = getStanfordPOSResults(
											stanford_maxent_POS_tagger, s);
									// System.out.println(stanfordPOSs.size());
									// System.out.println(tokens.size());

									dragonSentence = new dragon.nlp.Sentence();
									for (int j = 0; j < size; j++)
										dragonSentence.addWord(new Word(
												tokens.get(j).getText()));
									dragonTool_pos_tagger.tag(dragonSentence);

									for (int k = 0; k < size; k++) {
										int posIndex = dragonSentence.getWord(k)
												.getPOSIndex();
										String posName = getMappingNameFromHeppleTagger(
												posIndex);

										Token token = tokens.get(k);
										token.setPos(posName);
										token.setStanfordPOS(
												stanfordPOSs.get(k));
										// System.out.println(stanfordPOSs);
										// System.out.println(token.getText() +
										// " " + stanfordPOSs.get(k));
										token.setLemma(
												dragonTool_lemmatiser.lemmatize(
														token.getText()
																.toLowerCase(),
														posIndex));
									}

									sentences.add(s);
									this.sentences.add(s); // Dataset中的sentences
								}
								continue;
							}

							// chemical mention or disease mention
							if (currentLine.indexOf("\tDisease\t") != -1
									|| currentLine
											.indexOf("\tChemical\t") != -1) {
								String[] ss = currentLine.split("\\t");
								if (ss.length > 7) {
									System.err.println(currentLine);
									throw new IllegalStateException(
											"Mention counts error!");
								}

								// String documentID = ss[0];
								if (!ss[0].equals(docid)) {// docid
									throw new IllegalStateException(
											"The docid between title and disease does not match!");
								}

								int startOffInDoc = Integer.valueOf(ss[1]);
								int endOffInDoc = Integer.valueOf(ss[2]);
								String text = ss[3];// mention text
								String entityTypeName = ss[4];
								String conceptIDs[] = ss[5].split("\\|"); // 一个mention有多个conceptID
								Sentence tempSentence = null;
								for (Sentence sent : sentences) {
									// System.out.println(start+ " " + end + " "
									// + text);
									// System.out.println(sent);
									if (sent.isContainsText(startOffInDoc,
											endOffInDoc, text)) {
										tempSentence = sent;
										break;
									}
								}

								if (ss.length == 7) {
									String nickyNames[] = ss[6].split("\\|");
									for (int i = 0; i < conceptIDs.length; i++) {
										// System.out.println(start + " " + end
										// +
										// " "
										// + text);
										Mention m = new Mention(
												tempSentence.getDocumentID(),
												tempSentence.getSentenceIndex(),
												startOffInDoc, endOffInDoc,
												text,
												new EntityType(entityTypeName));
										m.setConceptID(conceptIDs[i]);
										if (i < nickyNames.length)
											m.setNickyName(nickyNames[i]);
										tempSentence.addMention(m);
										// System.out.println(m);

										mentions.add(m);
									}
								} else {
									for (int i = 0; i < conceptIDs.length; i++) {
										// System.out.println(start + " " + end
										// +
										// " "
										// + text);
										Mention m = new Mention(
												tempSentence.getDocumentID(),
												tempSentence.getSentenceIndex(),
												startOffInDoc, endOffInDoc,
												text,
												new EntityType(entityTypeName));
										m.setConceptID(conceptIDs[i]);

										tempSentence.addMention(m);
										// System.out.println(m);

										mentions.add(m);
									}
								}

								continue;
							}

							// relation
							if (currentLine.indexOf("\tCID\t") != -1) {
								String[] ss = currentLine.split("\\t");
								if (ss.length != 4) {
									System.err.println(currentLine);
									throw new IllegalStateException(
											"CID relation has wrong number of components!");
								}
								String documentID = ss[0];
								String relationType = ss[1];
								String chemicalConceptID = ss[2];
								String diseaseConceptID = ss[3];

								Relation rel = new Relation(documentID, null,
										relationType, chemicalConceptID,
										diseaseConceptID);

								// System.out.println(rel);
								relations.add(rel);
								continue;
							}
							// System.out.println(tmp);
						}
						if (docid != null && title != null && abText != null) {
							Abstract ab = new Abstract(docid, title, abText);
							ab.addMentions(mentions);
							ab.addSentences(sentences);
							ab.addRelations(relations);
							ab.addAllMentions2Coreferences();
							abstracts.add(ab);
						}
					} else {
						abstractQueue.add(line);
					}
					line = rd.readLine();
				}

				if (!abstractQueue.isEmpty()) {
					String docid = null;
					String title = null;
					String abText = null;
					List<Sentence> sentences = new ArrayList<Sentence>();
					List<Mention> mentions = new ArrayList<Mention>();
					List<Relation> relations = new ArrayList<Relation>();

					while (!abstractQueue.isEmpty()) {
						String currentLn = abstractQueue.poll();

						// title
						if (currentLn.indexOf("|t|") != -1) {
							String[] ss = currentLn.split("\\|t\\|");
							docid = ss[0];
							title = ss[1];
							continue;
						}

						// abstract
						if (currentLn.indexOf("|a|") != -1) {
							String[] ss = currentLn.split("\\|a\\|");
							if (!ss[0].equals(docid)) {
								throw new IllegalStateException(
										"The docid between title and abstract does not match!");
							}
							abText = ss[1];

							// ssplit
							sentence_breaker.setText(title, abText);
							sentence_breaker
									.segementSentencesWithPostProcessing();
							List<String> sentenceTexts = sentence_breaker
									.getSentences();
							List<Integer> sentOffs = sentence_breaker
									.getSentenceStartOffset();
							assert (sentenceTexts.size() == sentOffs.size());

							for (int i = 0; i < sentOffs.size(); i++) {
								Sentence s = new Sentence(docid, i,
										sentenceTexts.get(i), sentOffs.get(i));

								// addToken
								tokenizer.tokenize(s);
								// System.out.println(s);
								// System.out.println(s.getTokens().size());

								// pos & lemma
								dragon.nlp.Sentence posSentence = null;
								List<Token> tokens = s.getTokens();
								int size = tokens.size();
								List<String> stanfordPOSs = getStanfordPOSResults(
										stanford_maxent_POS_tagger, s);
								// System.out.println(stanfordPOSs.size());
								// System.out.println(tokens.size());

								posSentence = new dragon.nlp.Sentence();
								for (int j = 0; j < size; j++)
									posSentence.addWord(
											new Word(tokens.get(j).getText()));
								dragonTool_pos_tagger.tag(posSentence);

								for (int k = 0; k < size; k++) {
									int posIndex = posSentence.getWord(k)
											.getPOSIndex();
									String posName = getMappingNameFromHeppleTagger(
											posIndex);

									Token token = tokens.get(k);
									token.setPos(posName);
									token.setStanfordPOS(stanfordPOSs.get(k));
									// System.out.println(stanfordPOSs);
									// System.out.println(token.getText() +
									// " " + stanfordPOSs.get(k));
									token.setLemma(
											dragonTool_lemmatiser.lemmatize(
													token.getText()
															.toLowerCase(),
													posIndex));
								}

								sentences.add(s);
								this.sentences.add(s);
							}
							continue;
						}

						// chemical mention or disease mention
						if (currentLn.indexOf("\tDisease\t") != -1
								|| currentLn.indexOf("\tChemical\t") != -1) {
							String[] ss = currentLn.split("\\t");
							if (ss.length > 7) {
								System.err.println(currentLn);
								throw new IllegalStateException(
										"Mention counts error!");
							}

							// String documentID = ss[0];
							if (!ss[0].equals(docid)) {// docid
								throw new IllegalStateException(
										"The docid between title and disease does not match!");
							}

							int start = Integer.valueOf(ss[1]);
							int end = Integer.valueOf(ss[2]);
							String text = ss[3];// mention text
							String entityTypeName = ss[4];
							String conceptIDs[] = ss[5].split("\\|");
							Sentence sentTmp = null;
							for (Sentence sent : sentences) {
								if (sent.isContainsText(start, end, text)) {
									sentTmp = sent;
									break;
								}
							}

							if (ss.length == 7) {
								String nickyNames[] = ss[6].split("\\|");
								for (int i = 0; i < conceptIDs.length; i++) {
									// System.out.println(start + " " + end
									// +
									// " "
									// + text);
									Mention m = new Mention(
											sentTmp.getDocumentID(),
											sentTmp.getSentenceIndex(), start,
											end, text,
											new EntityType(entityTypeName));
									m.setConceptID(conceptIDs[i]);
									if (i < nickyNames.length)
										m.setNickyName(nickyNames[i]);
									sentTmp.addMention(m);
									// System.out.println(m);

									mentions.add(m);
								}
							} else {
								for (int i = 0; i < conceptIDs.length; i++) {
									// System.out.println(start + " " + end
									// +
									// " "
									// + text);
									Mention m = new Mention(
											sentTmp.getDocumentID(),
											sentTmp.getSentenceIndex(), start,
											end, text,
											new EntityType(entityTypeName));
									m.setConceptID(conceptIDs[i]);

									sentTmp.addMention(m);
									// System.out.println(m);

									mentions.add(m);
								}
							}

							continue;
						}

						// relation
						if (currentLn.indexOf("\tCID\t") != -1) {
							String[] ss = currentLn.split("\\t");
							if (ss.length != 4) {
								System.err.println(currentLn);
								throw new IllegalStateException(
										"CID relation has wrong number of components!");
							}
							String documentID = ss[0];
							String relationType = ss[1];
							String chemicalConceptID = ss[2];
							String diseaseConceptID = ss[3];

							Relation rel = new Relation(documentID, null,
									relationType, chemicalConceptID,
									diseaseConceptID);

							// System.out.println(rel);
							relations.add(rel);
							continue;
						}
						// System.out.println(tmp);

					}
					if (docid != null && title != null && abText != null) {
						Abstract ab = new Abstract(docid, title, abText);
						ab.addMentions(mentions);
						ab.addSentences(sentences);
						ab.addRelations(relations);
						ab.addAllMentions2Coreferences();

						abstracts.add(ab);
					}
				}

				// parsing
				if (ifUseStanfordCoreNLPParser) {
					LexicalizedParser lp = getStanfordParser(
							STANFORD_PARSER_MODEL_NAME);

					for (Sentence s : this.sentences) {
						System.out.println("\n" + s.getMentionConcealedText());
						Tree parse = getParseResult(lp,
								s.getMentionConcealedText());
						s.setSyntacticParseTree(parse.toString());
					}
				}
			} finally {
				System.out.println("Loading file number: " + abstracts.size());
				rd.close();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		getStatisticInformation(pubtator_file.getName());

		writeDataset(output_folder, pubtator_file.getName());
	}

	// according to the dataset name, loading the dataset from local files
	// specify using bllip parser or stanford parser
	public void loadDatasetFromLocalFiles(File loading_folder,
			String original_dataset_name,
			boolean whether_loading_bllipparsing_file,
			boolean whether_loading_relation_annotation) {
		if (!loading_folder.exists() || !loading_folder.isDirectory())
			throw new IllegalArgumentException(
					"The file folder does not exist!");

		// File dir = new File(methodRootDirectory);
		// File datasetDir = new File(dir.getPath() + "/" + "dataset");
		// String prefix = datasetDir.getPath() + "/";

		// tokens
		File token_file = new File(loading_folder.getPath() + "/"
				+ original_dataset_name + ".token");
		if (!token_file.exists() || !token_file.isFile())
			throw new IllegalArgumentException(
					"The token file does not exist!");
		Map<String, List<Token>> tokensInFile = readTokenFile(token_file,
				"utf-8");

		// mentions
		File mention_file = new File(loading_folder.getPath() + "/"
				+ original_dataset_name + ".mention");
		if (!mention_file.exists() || !mention_file.isFile())
			throw new IllegalArgumentException(
					"The mention file does not exist!");
		Map<String, List<Mention>> mentionsInFile = readMentionFile(
				mention_file, "utf-8");

		// sentences
		File sentence_file = new File(loading_folder.getPath() + "/"
				+ original_dataset_name + ".sentence");
		if (!sentence_file.exists() || !sentence_file.isFile())
			throw new IllegalArgumentException(
					"The sentence file does not exist!");
		Map<String, List<Sentence>> sentencesInFile = readSentenceFile(
				sentence_file, "utf-8");

		// relations
		File relation_file = new File(loading_folder.getPath() + "/"
				+ original_dataset_name + ".relation");
		if (!relation_file.exists() || !relation_file.isFile())
			throw new IllegalArgumentException(
					"The relation file does not exist!");
		Map<String, List<Relation>> relationsInFile = readRelationFile(
				relation_file, "utf-8");

		// McClosky parsing file
		Map<String, List<String>> syntacticParsingLinesInFile = null;
		if (whether_loading_bllipparsing_file) {
			File syntactic_parsing_file = new File(loading_folder.getPath()
					+ "/" + original_dataset_name + ".Mcclosky");
			if (!syntactic_parsing_file.exists()
					|| !syntactic_parsing_file.isFile())
				throw new IllegalArgumentException(
						"The bllip parsing file does not exist!");
			syntacticParsingLinesInFile = readSyntacticParsingFile(
					syntactic_parsing_file, "utf-8");
		} else {
			File syntactic_parsing_file = new File(loading_folder.getPath()
					+ "/" + original_dataset_name + ".syntacticParsing");
			if (!syntactic_parsing_file.exists()
					|| !syntactic_parsing_file.isFile())
				throw new IllegalArgumentException(
						"The syntactic parsing file does not exist!");
			syntacticParsingLinesInFile = readSyntacticParsingFile(
					syntactic_parsing_file, "utf-8");
		}

		// abstracts
		File abstract_file = new File(loading_folder.getPath() + "/"
				+ original_dataset_name + ".abstract");
		if (!abstract_file.exists() || !abstract_file.isFile())
			throw new IllegalArgumentException(
					"The abstract file does not exist!");
		Map<String, Abstract> abstractsInFile = readAbstractFile(abstract_file,
				"utf-8");

		// restore sentences
		for (Map.Entry<String, List<Sentence>> entry : sentencesInFile
				.entrySet()) {
			String docid = entry.getKey();
			List<Sentence> sentences = entry.getValue();
			List<Mention> mentions = mentionsInFile.get(docid);
			List<Token> tokens = tokensInFile.get(docid);
			List<String> syntacticParsingLines = syntacticParsingLinesInFile
					.get(docid);

			for (Sentence s : sentences) {
				this.sentences.add(s);

				int sentenceIndex = s.getSentenceIndex();
				for (Token t : tokens) {
					if (t.getSentenceIndex() == sentenceIndex) {
						s.addToken(t);
					}
				}
				for (Mention m : mentions) {
					if (m.getSentenceIndex() == sentenceIndex) {
						s.addMention(m);
					}
				}
				for (String l : syntacticParsingLines) {
					String component[] = l.split("\\t");
					if (Integer.valueOf(component[1]) == sentenceIndex) {
						s.setSyntacticParseTree(component[2]);
					}
				}
			}
		}

		// restore abstracts
		List<Abstract> abs = new ArrayList<Abstract>();
		for (Map.Entry<String, Abstract> entry : abstractsInFile.entrySet()) {
			String docid = entry.getKey();
			Abstract ab = entry.getValue();
			List<Sentence> sentences = sentencesInFile.get(docid);
			ab.addSentences(sentences);
			List<Mention> mentions = mentionsInFile.get(docid);
			ab.addMentions(mentions);

			if (whether_loading_relation_annotation) {// test set可能没有relation
				List<Relation> relations = relationsInFile.get(docid);
				// if(relations==null)
				// System.out.println(docid);
				ab.addRelations(relations);
				for (Relation r : relations)
					r.setAbstract(ab);
			}

			ab.addAllMentions2Coreferences();
			abs.add(ab);
		}

		setAbstracts(abs);

		// output some statistics
		getStatisticInformation(original_dataset_name);
	}

	private Map<String, List<Token>> readTokenFile(File token_file,
			String encoding) {
		if (!token_file.exists() || !token_file.isFile())
			throw new IllegalArgumentException(
					"The token file does not exist!");

		Map<String, List<Token>> map = new HashMap<String, List<Token>>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(token_file, encoding);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					if (line.isEmpty())
						continue;
					// System.out.println(word);
					String component[] = line.split("\\t");
					Token t = new Token(component[0],
							Integer.valueOf(component[1]),
							Integer.valueOf(component[3]),
							Integer.valueOf(component[4]), component[2],
							component[5], component[6], component[7]);

					if (map.containsKey(component[0])) {// docid
						map.get(component[0]).add(t);
					} else {
						List<Token> tokens = new ArrayList<Token>();
						tokens.add(t);
						map.put(component[0], tokens);
					}
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}

	private Map<String, List<Mention>> readMentionFile(File mention_file,
			String encoding) {
		if (!mention_file.exists() || !mention_file.isFile())
			throw new IllegalArgumentException(
					"The mention file does not exist!");

		Map<String, List<Mention>> map = new HashMap<String, List<Mention>>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(mention_file, encoding);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					if (line.isEmpty())
						continue;
					String component[] = line.split("\\t");
					Mention m = null;
					if (component.length == 9) {
						m = new Mention(component[0],
								Integer.valueOf(component[1]),
								Integer.valueOf(component[4]),
								Integer.valueOf(component[5]), component[2],
								new EntityType(component[3]), null,
								component[8], null);
					}
					if (component.length == 10) {
						m = new Mention(component[0],
								Integer.valueOf(component[1]),
								Integer.valueOf(component[4]),
								Integer.valueOf(component[5]), component[2],
								new EntityType(component[3]), null,
								component[8], component[9]);
					}

					if (map.containsKey(component[0])) {// docid
						map.get(component[0]).add(m);
					} else {
						List<Mention> mentions = new ArrayList<Mention>();
						mentions.add(m);
						map.put(component[0], mentions);
					}
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}

	private Map<String, List<Relation>> readRelationFile(File relation_file,
			String encoding) {
		if (!relation_file.exists() || !relation_file.isFile())
			throw new IllegalArgumentException(
					"The relation file does not exist!");

		Map<String, List<Relation>> map = new HashMap<String, List<Relation>>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(relation_file, encoding);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					if (line.isEmpty())
						continue;
					// System.out.println(word);
					String component[] = line.split("\\t");
					Relation r = new Relation(component[0], null, component[1],
							component[2], component[3]);

					if (map.containsKey(component[0])) {// docid
						map.get(component[0]).add(r);
					} else {
						List<Relation> relations = new ArrayList<Relation>();
						relations.add(r);
						map.put(component[0], relations);
					}
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}

	private Map<String, List<Sentence>> readSentenceFile(File sentence_file,
			String encoding) {
		if (!sentence_file.exists() || !sentence_file.isFile())
			throw new IllegalArgumentException(
					"The sentence file does not exist!");

		Map<String, List<Sentence>> map = new HashMap<String, List<Sentence>>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(sentence_file, encoding);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					if (line.isEmpty())
						continue;
					String component[] = line.split("\\t");
					Sentence s = new Sentence(component[0],
							Integer.valueOf(component[1]), component[3],
							Integer.valueOf(component[2]));

					if (map.containsKey(component[0])) {// docid
						map.get(component[0]).add(s);
					} else {
						List<Sentence> sentences = new ArrayList<Sentence>();
						sentences.add(s);
						map.put(component[0], sentences);
					}
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}

	private Map<String, List<String>> readSyntacticParsingFile(
			File syntactic_file, String encoding) {
		if (!syntactic_file.exists() || !syntactic_file.isFile())
			throw new IllegalArgumentException(
					"The syntactic parsing file does not exist!");

		Map<String, List<String>> map = new HashMap<String, List<String>>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(syntactic_file, encoding);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					if (line.isEmpty())
						continue;
					String component[] = line.split("\\t");

					if (map.containsKey(component[0])) {// docid
						map.get(component[0]).add(line);
					} else {
						List<String> lines = new ArrayList<String>();
						lines.add(line);
						map.put(component[0], lines);
					}
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}

	private Map<String, Abstract> readAbstractFile(File abstract_file,
			String encoding) {
		if (!abstract_file.exists() || !abstract_file.isFile())
			throw new IllegalArgumentException(
					"The abstract file does not exist!");

		Map<String, Abstract> abs = new TreeMap<String, Abstract>();
		Scanner in = null;
		try {
			try {
				in = new Scanner(abstract_file, encoding);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					if (line.isEmpty())
						continue;
					String component[] = line.split("\\t");
					Abstract a = new Abstract(component[0], component[1],
							component[2]);

					abs.put(component[0], a);
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return abs;
	}

	@Override
	public void load(HierarchicalConfiguration config) {
		throw new NotImplementedException();
	}

	@Override
	public List<Dataset> split(int n) {
		throw new NotImplementedException();
	}

	public void getStatisticInformation(String dataset_name) {
		int tokenNumber = 0;
		int sentenceNumber = sentences.size();
		int abstractNumber = abstracts.size();
		int mentionNumber = 0;
		int relationNumber = 0;
		int validRelationNumber = 0;
		int both_Relation_Concepts_Can_Occur_In_One_Sentence = 0;
		Set<String> tokenLiteralTexts = new HashSet<String>();

		for (Abstract a : abstracts) {
			List<Sentence> sents = a.getSentences();
			mentionNumber += a.getMentions().size();
			List<Relation> rels = a.getRelations();
			relationNumber += rels.size();

			for (Relation r : rels) {
				if (r.canRelationGetCorrespondingMentions()) {
					validRelationNumber++;
				}
				// else
				// System.out.println("000000000000000000" + r);

				if (r.hasCooccurrenceInOneSentence()) {
					both_Relation_Concepts_Can_Occur_In_One_Sentence++;
				}
			}

			for (Sentence s : sents) {
				List<Token> tokens = s.getTokens();
				tokenNumber += tokens.size();
				for (Token t : tokens)
					tokenLiteralTexts.add(t.getText());
			}
		}

		System.out.println("\n******** Statistics *********");
		System.out.println("Dataset name is: " + dataset_name);
		System.out.println("Abstracts number is: " + abstractNumber);
		System.out.println("Tokens number is: " + tokenNumber);
		System.out.println(
				"UnDuplicated Tokens number is: " + tokenLiteralTexts.size());
		System.out.println("Sentences number is: " + sentenceNumber);
		System.out.println("Mentions number is: " + mentionNumber);
		System.out.println("Relations number is: " + relationNumber);
		System.out.println("Valid Relations number is: " + validRelationNumber);
		System.out.println(
				"The number of Relations that happen in one sentence is: "
						+ both_Relation_Concepts_Can_Occur_In_One_Sentence);

	}

	public List<Relation> formNewRelaionsOfTheDataset() {
		List<Relation> newRelations = new ArrayList<Relation>();
		for (Abstract a : abstracts) {
			List<Relation> rels = a.formNewRelations();
			newRelations.addAll(rels);
		}

		System.out.println("The number of the new relations of the dataset is： "
				+ newRelations.size());
		return newRelations;
	}

	public Set<Token> getTokens() {
		Set<Token> tokens = new TreeSet<Token>();
		for (Sentence s : this.sentences) {
			List<Token> sentTokens = s.getTokens();
			tokens.addAll(sentTokens);
		}
		return Collections.unmodifiableSet(tokens);
	}

	// load one file in Pubtator format once
	public void loadFromOnePubtatorFile(File file, String encoding) {
		if (!file.exists() || !file.isFile())
			return;

		BufferedReader rd = null;
		ArrayDeque<String> abstractQueue = new ArrayDeque<String>();
		try {
			try {
				rd = new BufferedReader(new InputStreamReader(
						new FileInputStream(file.getPath()), encoding));
				String line = rd.readLine();

				while (line != null) {
					if (line.isEmpty()) {
						String docid = null;
						String title = null;
						String abText = null;
						List<Sentence> sentences = new ArrayList<Sentence>();
						List<Mention> mentions = new ArrayList<Mention>();
						List<Relation> relations = new ArrayList<Relation>();

						while (!abstractQueue.isEmpty()) {
							String currentLine = abstractQueue.poll();
							// System.out.println(tmp);

							// title
							if (currentLine.indexOf("|t|") != -1) {
								String[] ss = currentLine.split("\\|t\\|");
								docid = ss[0];
								title = ss[1];
								continue;
							}

							// abstract
							if (currentLine.indexOf("|a|") != -1) {
								String[] ss = currentLine.split("\\|a\\|");
								if (!ss[0].equals(docid)) {
									throw new IllegalStateException(
											"The docid between title and abstract does not match!");
								}
								abText = ss[1];

								// ssplit
								sentence_breaker.setText(title, abText);
								sentence_breaker
										.segementSentencesWithPostProcessing();
								List<String> sentenceTexts = sentence_breaker
										.getSentences();
								List<Integer> sentOffs = sentence_breaker
										.getSentenceStartOffset();
								assert (sentenceTexts.size() == sentOffs
										.size());

								for (int i = 0; i < sentOffs.size(); i++) {
									Sentence s = new Sentence(docid, i,
											sentenceTexts.get(i),
											sentOffs.get(i));

									// addToken
									tokenizer.tokenize(s);
									// System.out.println(s);
									// System.out.println(s.getTokens().size());

									// pos & lemma
									dragon.nlp.Sentence dragonSentence = null;
									List<Token> tokens = s.getTokens();
									int size = tokens.size();
									List<String> stanfordPOSs = getStanfordPOSResults(
											stanford_maxent_POS_tagger, s);
									// System.out.println(stanfordPOSs.size());
									// System.out.println(tokens.size());

									dragonSentence = new dragon.nlp.Sentence();
									for (int j = 0; j < size; j++)
										dragonSentence.addWord(new Word(
												tokens.get(j).getText()));
									dragonTool_pos_tagger.tag(dragonSentence);

									for (int k = 0; k < size; k++) {
										int posIndex = dragonSentence.getWord(k)
												.getPOSIndex();
										String posName = getMappingNameFromHeppleTagger(
												posIndex);

										Token token = tokens.get(k);
										token.setPos(posName);
										token.setStanfordPOS(
												stanfordPOSs.get(k));
										// System.out.println(stanfordPOSs);
										// System.out.println(token.getText() +
										// " " + stanfordPOSs.get(k));
										token.setLemma(
												dragonTool_lemmatiser.lemmatize(
														token.getText()
																.toLowerCase(),
														posIndex));
									}

									sentences.add(s);
									this.sentences.add(s); // Dataset中的sentences
								}
								continue;
							}

							// chemical mention or disease mention
							if (currentLine.indexOf("\tDisease\t") != -1
									|| currentLine
											.indexOf("\tChemical\t") != -1) {
								String[] ss = currentLine.split("\\t");
								if (ss.length > 7) {
									System.err.println(currentLine);
									throw new IllegalStateException(
											"Mention counts error!");
								}

								// String documentID = ss[0];
								if (!ss[0].equals(docid)) {// docid
									throw new IllegalStateException(
											"The docid between title and disease does not match!");
								}

								int startOffInDoc = Integer.valueOf(ss[1]);
								int endOffInDoc = Integer.valueOf(ss[2]);
								String text = ss[3];// mention text
								String entityTypeName = ss[4];
								String conceptIDs[] = ss[5].split("\\|"); // some
																			// mentions
																			// have
																			// multiple
																			// conceptIDs
								Sentence tempSentence = null;
								for (Sentence sent : sentences) {
									// System.out.println(start+ " " + end + " "
									// + text);
									// System.out.println(sent);
									if (sent.isContainsText(startOffInDoc,
											endOffInDoc, text)) {
										tempSentence = sent;
										break;
									}
								}

								if (ss.length == 7) {// when a mention has
														// multiple conceptIDs,
														// it may have multiple
														// aliases

									String nickyNames[] = ss[6].split("\\|");
									for (int i = 0; i < conceptIDs.length; i++) {
										// System.out.println(start + " " + end
										// +
										// " "
										// + text);
										Mention m = new Mention(
												tempSentence.getDocumentID(),
												tempSentence.getSentenceIndex(),
												startOffInDoc, endOffInDoc,
												text,
												new EntityType(entityTypeName));
										m.setConceptID(conceptIDs[i]);
										if (i < nickyNames.length)
											m.setNickyName(nickyNames[i]);
										tempSentence.addMention(m);
										// System.out.println(m);

										mentions.add(m);
									}
								} else {
									for (int i = 0; i < conceptIDs.length; i++) {
										// System.out.println(start + " " + end
										// +
										// " "
										// + text);
										Mention m = new Mention(
												tempSentence.getDocumentID(),
												tempSentence.getSentenceIndex(),
												startOffInDoc, endOffInDoc,
												text,
												new EntityType(entityTypeName));
										m.setConceptID(conceptIDs[i]);

										tempSentence.addMention(m);
										// System.out.println(m);

										mentions.add(m);
									}
								}
								continue;
							}

							// relation
							if (currentLine.indexOf("\tCID\t") != -1) {
								String[] ss = currentLine.split("\\t");
								if (ss.length != 4) {
									System.err.println(currentLine);
									throw new IllegalStateException(
											"CID relation has wrong number of components!");
								}
								String documentID = ss[0];
								String relationType = ss[1];
								String chemicalConceptID = ss[2];
								String diseaseConceptID = ss[3];

								Relation rel = new Relation(documentID, null,
										relationType, chemicalConceptID,
										diseaseConceptID);

								// System.out.println(rel);
								relations.add(rel);
								continue;
							}
							// System.out.println(tmp);
						}
						if (docid != null && title != null && abText != null) {
							Abstract ab = new Abstract(docid, title, abText);
							ab.addMentions(mentions);
							ab.addSentences(sentences);
							ab.addRelations(relations);
							ab.addAllMentions2Coreferences();
							abstracts.add(ab);
						}
					} else {
						abstractQueue.add(line);
					}
					line = rd.readLine();
				}

				// the queue is not empty
				if (!abstractQueue.isEmpty()) {
					String docid = null;
					String title = null;
					String abText = null;
					List<Sentence> sentences = new ArrayList<Sentence>();
					List<Mention> mentions = new ArrayList<Mention>();
					List<Relation> relations = new ArrayList<Relation>();

					while (!abstractQueue.isEmpty()) {
						String currentLn = abstractQueue.poll();

						// title
						if (currentLn.indexOf("|t|") != -1) {
							String[] ss = currentLn.split("\\|t\\|");
							docid = ss[0];
							title = ss[1];
							continue;
						}

						// abstract
						if (currentLn.indexOf("|a|") != -1) {
							String[] ss = currentLn.split("\\|a\\|");
							if (!ss[0].equals(docid)) {
								throw new IllegalStateException(
										"The docid between title and abstract does not match!");
							}
							abText = ss[1];

							// ssplit
							sentence_breaker.setText(title, abText);
							sentence_breaker
									.segementSentencesWithPostProcessing();
							List<String> sentenceTexts = sentence_breaker
									.getSentences();
							List<Integer> sentOffs = sentence_breaker
									.getSentenceStartOffset();
							assert (sentenceTexts.size() == sentOffs.size());

							for (int i = 0; i < sentOffs.size(); i++) {
								Sentence s = new Sentence(docid, i,
										sentenceTexts.get(i), sentOffs.get(i));

								// addToken
								tokenizer.tokenize(s);
								// System.out.println(s);
								// System.out.println(s.getTokens().size());

								// pos & lemma
								dragon.nlp.Sentence posSentence = null;
								List<Token> tokens = s.getTokens();
								int size = tokens.size();
								List<String> stanfordPOSs = getStanfordPOSResults(
										stanford_maxent_POS_tagger, s);
								// System.out.println(stanfordPOSs.size());
								// System.out.println(tokens.size());

								posSentence = new dragon.nlp.Sentence();
								for (int j = 0; j < size; j++)
									posSentence.addWord(
											new Word(tokens.get(j).getText()));
								dragonTool_pos_tagger.tag(posSentence);

								for (int k = 0; k < size; k++) {
									int posIndex = posSentence.getWord(k)
											.getPOSIndex();
									String posName = getMappingNameFromHeppleTagger(
											posIndex);

									Token token = tokens.get(k);
									token.setPos(posName);
									token.setStanfordPOS(stanfordPOSs.get(k));
									// System.out.println(stanfordPOSs);
									// System.out.println(token.getText() +
									// " " + stanfordPOSs.get(k));
									token.setLemma(
											dragonTool_lemmatiser.lemmatize(
													token.getText()
															.toLowerCase(),
													posIndex));
								}
								sentences.add(s);
								this.sentences.add(s); // Dataset中的sentences
							}
							continue;
						}

						// chemical mention or disease mention
						if (currentLn.indexOf("\tDisease\t") != -1
								|| currentLn.indexOf("\tChemical\t") != -1) {
							String[] ss = currentLn.split("\\t");
							if (ss.length > 7) {
								System.err.println(currentLn);
								throw new IllegalStateException(
										"Mention counts error!");
							}

							// String documentID = ss[0];
							if (!ss[0].equals(docid)) {// docid
								throw new IllegalStateException(
										"The docid between title and disease does not match!");
							}

							int start = Integer.valueOf(ss[1]);
							int end = Integer.valueOf(ss[2]);
							String text = ss[3];// mention text
							String entityTypeName = ss[4];
							String conceptIDs[] = ss[5].split("\\|"); // 一个mention有多个conceptID
							Sentence sentTmp = null;
							for (Sentence sent : sentences) {
								if (sent.isContainsText(start, end, text)) {
									sentTmp = sent;
									break;
								}
							}

							if (ss.length == 7) {
								String nickyNames[] = ss[6].split("\\|");
								for (int i = 0; i < conceptIDs.length; i++) {
									// System.out.println(start + " " + end
									// +
									// " "
									// + text);
									Mention m = new Mention(
											sentTmp.getDocumentID(),
											sentTmp.getSentenceIndex(), start,
											end, text,
											new EntityType(entityTypeName));
									m.setConceptID(conceptIDs[i]);
									if (i < nickyNames.length)
										m.setNickyName(nickyNames[i]);
									sentTmp.addMention(m);
									// System.out.println(m);

									mentions.add(m);
								}
							} else {
								for (int i = 0; i < conceptIDs.length; i++) {
									// System.out.println(start + " " + end
									// +
									// " "
									// + text);
									Mention m = new Mention(
											sentTmp.getDocumentID(),
											sentTmp.getSentenceIndex(), start,
											end, text,
											new EntityType(entityTypeName));
									m.setConceptID(conceptIDs[i]);

									sentTmp.addMention(m);
									// System.out.println(m);

									mentions.add(m);
								}
							}
							continue;
						}

						// relation
						if (currentLn.indexOf("\tCID\t") != -1) {
							String[] ss = currentLn.split("\\t");
							if (ss.length != 4) {
								System.err.println(currentLn);
								throw new IllegalStateException(
										"CID relation has wrong number of components!");
							}
							String documentID = ss[0];
							String relationType = ss[1];
							String chemicalConceptID = ss[2];
							String diseaseConceptID = ss[3];

							Relation rel = new Relation(documentID, null,
									relationType, chemicalConceptID,
									diseaseConceptID);

							// System.out.println(rel);
							relations.add(rel);
							continue;
						}
						// System.out.println(tmp);

					}
					if (docid != null && title != null && abText != null) {
						Abstract ab = new Abstract(docid, title, abText);
						ab.addMentions(mentions);
						ab.addSentences(sentences);
						ab.addRelations(relations);
						ab.addAllMentions2Coreferences();

						abstracts.add(ab);
					}
				}

				// // parsing
				// if (ifUseStanfordCoreNLPParser) {
				// LexicalizedParser lp = getStanfordParser(
				// stanfordParserModelName);
				//
				// for (Sentence s : this.sentences) {
				// System.out.println("\n" + s.getMentionConcealedText());
				// Tree parse = getParseResult(lp,
				// s.getMentionConcealedText());
				// s.setSyntacticParseTree(parse.toString());
				// }
				// }
			} finally {
				System.out.println("Loading file number: " + abstracts.size());
				rd.close();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		getStatisticInformation(file.getName());

		String filename = file.getName();
		String filename_prefix = filename.substring(0,
				filename.lastIndexOf('.'));
		// File datasetDir = new File("./temp");
		// writeDataset(datasetDir.getPath(), filename + ".abstract",
		// filename + ".sentence", filename + ".mention",
		// filename + ".relation", filename + ".token",
		// filename + ".tokenizedSentence", filename + ".syntacticParsing",
		// filename + ".dependency", filename + ".wordsStatistic",
		// filename + ".mentionConcealedSentence");

		File bllip_parsing_input_directory = new File("temp/parsing_input");
		File bllip_parsing_output_directory = new File("temp/parsing_output");
		if (!bllip_parsing_input_directory.exists()
				|| !bllip_parsing_input_directory.isDirectory()) {
			bllip_parsing_input_directory.mkdir();
		}
		if (!bllip_parsing_output_directory.exists()
				|| !bllip_parsing_output_directory.isDirectory()) {
			bllip_parsing_output_directory.mkdir();
		}

		List<Sentence> all_sentences = new ArrayList<Sentence>();
		PrintWriter outMentionConcealedSentencesForMccloskyParser = null;
		try {
			try {
				outMentionConcealedSentencesForMccloskyParser = new PrintWriter(
						bllip_parsing_input_directory.getPath() + "/"
								+ filename_prefix + ".SentenceForMcclosky",
						"utf-8");

				List<Abstract> abs = this.getAbstracts();
				Collections.sort(abs);
				for (Abstract a : abs) {
					List<Sentence> sentences = a.getSentences();
					for (Sentence s : sentences) {
						all_sentences.add(s);
						outMentionConcealedSentencesForMccloskyParser.println(
								"<s> " + s.getMentionConcealedText() + " </s>");
					}
				}
				outMentionConcealedSentencesForMccloskyParser.flush();
			} finally {
				outMentionConcealedSentencesForMccloskyParser.close();
			}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		Runtime rn = Runtime.getRuntime();
		Process p = null;
		try {
			List<String> cmds = new ArrayList<String>();
			cmds.add("sh");
			cmds.add("-c");
			cmds.add("test.sh");
			String cmd = "./bllip-parse.sh ./temp/parsing_input/"
					+ filename_prefix
					+ ".SentenceForMcclosky > ./temp/parsing_output/"
					+ filename_prefix + ".Bllip";
			p = rn.exec(new String[] { "/bin/sh", "-c", cmd });
			int code = p.waitFor();
			if (code == 0) {
				System.out.println("OK");
			} else {
				System.err.println("fail");
			}
			p.waitFor();
			System.out.println("Parsing is OK!");
		} catch (Exception e) {
			System.err.println("Error win exec!");
		}

		Scanner in = null;
		try {
			try {
				in = new Scanner(
						new File(bllip_parsing_output_directory.getPath() + "/"
								+ filename_prefix + ".Bllip"),
						"utf-8");
				int cn = 0;
				while (in.hasNextLine()) {
					String line = in.nextLine();
					// System.out.println(word);
					all_sentences.get(cn).setSyntacticParseTree(line);
					cn++;
				}
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		for (Sentence s : all_sentences) {
			System.out.println(s.getText());
			System.out.println(s.getSyntacticParsing());
			System.out.println();
		}
	}

	public static void main(String[] args) {
		GJHPubtatorDataset ttDataset = new GJHPubtatorDataset();
		ttDataset.loadDatasetFromLocalFiles(
				new File("DifferentMethods/SplitProcessing_MentionLevel/7777"),
				"CDR_TrainingSet.PubTator.txt", true, true);

		System.out.println("\n**** End ****\n");

		// isFileLineDuplicated("./Corpus/CDR_TestSet.PubTator.txt", "utf-8");
	}

}
