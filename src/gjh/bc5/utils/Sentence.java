package gjh.bc5.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.NPTmpRetainingTreeNormalizer;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.Trees;
import edu.stanford.nlp.trees.UniversalSemanticHeadFinder;
import gjh.bc5.dataset.SimpleTokenizer;
import gjh.bc5.dataset.Tokenizer;

public class Sentence extends SerialCloneable implements Comparable<Sentence> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2720610540021449665L;
	private String documentID;
	private int sentenceIndex;
	private String text;
	private Set<Token> tokens;
	private Set<Mention> mentions;
	// private Map<Mention, EntityIdentification> identifications;
	private int sentStartOffset;
	private String syntacticParsing;// parsing result in a line

	public Sentence() {
		documentID = "";
		sentenceIndex = -1;
		text = "";
		tokens = new TreeSet<Token>();
		mentions = new TreeSet<Mention>();
		sentStartOffset = -1;
		syntacticParsing = "";
	}

	// deep copy
	public Sentence(Sentence otherSentence) {
		documentID = otherSentence.getDocumentID();
		sentenceIndex = otherSentence.getSentenceIndex();
		text = otherSentence.getText();
		tokens = new TreeSet<Token>();
		for (Token t : otherSentence.getTokens()) {
			Token token = new Token(t);// deep copy
			this.tokens.add(token);
		}

		mentions = new TreeSet<Mention>();
		for (Mention m : otherSentence.getMentions()) {
			Mention mention = new Mention(m);// deep copy
			this.mentions.add(mention);
		}
		sentStartOffset = otherSentence.getSentStartOffset();

		this.syntacticParsing = otherSentence.syntacticParsing;
	}

	public Sentence(String documentID, int sentenceIndex, String text,
			Set<Token> tokens, Set<Mention> mentions, int sentStartOffset,
			String parseTree) {
		if (documentID == null || documentID.isEmpty())
			throw new IllegalArgumentException(
					"documentID cannot be null or empty!");
		this.documentID = documentID;

		if (sentenceIndex < 0)
			throw new IllegalArgumentException(
					"sentenceIndex should be larger than 0!");
		this.sentenceIndex = sentenceIndex;

		if (text == null)
			throw new IllegalArgumentException("Text cannot be null");
		text = text.trim();
		if (text.length() == 0)
			throw new IllegalArgumentException(
					"Text must have length greater than 0");
		this.text = text;

		if (tokens == null) {
			this.tokens = new TreeSet<Token>();
		} else {
			this.tokens = tokens;
		}

		if (mentions == null) {
			this.mentions = new TreeSet<Mention>();
		} else {
			this.mentions = mentions;
		}

		if (sentStartOffset < 0)
			throw new IllegalArgumentException(
					"The sentence must have startOffset greater than 0");
		this.sentStartOffset = sentStartOffset;

		if (parseTree == null) {
			this.syntacticParsing = "";
		} else {
			this.syntacticParsing = parseTree;
		}
	}

	public Sentence(String documentID, int sentenceIndex, String text,
			int sentStartOffset) {
		this(documentID, sentenceIndex, text, null, null, sentStartOffset,
				null);
	}

	public boolean isReady() {
		if (documentID.isEmpty() || sentenceIndex < 0 || sentStartOffset < 0
				|| text.isEmpty()) {
			return false;
		}
		return true;
	}

	public int getSentStartOffset() {
		return sentStartOffset;
	}

	public void setSentStartOffset(int sentStartOffset) {
		this.sentStartOffset = sentStartOffset;
	}

	public void setSentOffset(int offset) {
		if (offset < 0)
			throw new IllegalArgumentException(
					"The sentence must have startOffset greater than 0");

		sentStartOffset = offset;
	}

	/**
	 * Adds a {@link Token} to this {@link Sentence}. Normally called by
	 * instances of {@link Tokenizer}.
	 *
	 * @param token
	 */
	public void addToken(Token token) {
		// Add verification of no token overlap
		if (token.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException();
		if (!tokens.contains(token))
			tokens.add(token);
	}

	// counts of white space before specific charOffset in a sentence
	public int countWhitespace(int charOffsetInSentence) {
		int count = 0;
		charOffsetInSentence = Math.min(charOffsetInSentence, text.length());
		for (int i = 0; i < charOffsetInSentence; i++) {
			if (Character.isWhitespace(text.charAt(i)))
				count++;
		}
		return count;
	}

	/**
	 * Adds a {@link Mention} to this Sentence, ignoring any potential overlap
	 * with existing {@link Mention}s. Normally called by instance of
	 * {@link Tagger} or post-processors.
	 *
	 * @param mention
	 */
	public boolean addMention(Mention mention) {
		if (mention.getSentenceIndex() != this.sentenceIndex)
			throw new IllegalArgumentException(
					"The mention is not in this sentence!");
		for (Mention previous : mentions) {
			if (previous.equals(mention) && mention.getProbability() != null) {
				if (previous.getProbability() == null
						|| mention.getProbability() > previous.getProbability())
					previous.setProbability(mention.getProbability());
				return false;
			}
		}
		return mentions.add(mention);
	}

	public boolean removeMention(Mention mention) {
		return mentions.remove(mention);
	}

	public void addMentions(List<Mention> mentions) {
		this.mentions.addAll(mentions);
	}

	/**
	 * @return the tag for the {@link Sentence}
	 */
	public int getSentenceIndex() {
		return sentenceIndex;
	}

	public String getDocumentID() {
		return documentID;
	}

	/**
	 * @return The text of the {@link Sentence}
	 */
	public String getText() {
		return text;
	}

	public String getSyntacticParsing() {
		return syntacticParsing;
	}

	public void setSyntacticParseTree(String parseTree) {
		this.syntacticParsing = parseTree;
	}

	// get the dependency String of a sentence
	public String getTreePresentationString(String format) {
		String synParsing = this.getSyntacticParsing();
		System.err.println("processing: " + synParsing);

		BufferedReader br = new BufferedReader(new StringReader(synParsing));
		PennTreeReader ptr = new PennTreeReader(br,
				new LabeledScoredTreeFactory(),
				new NPTmpRetainingTreeNormalizer());

		Tree t = null;
		try {
			t = ptr.readTree();
		} catch (IOException e) {
			e.printStackTrace();
		}

		StringWriter sw = new StringWriter();
		BufferedWriter bw = new BufferedWriter(sw);
		PrintWriter out = new PrintWriter(bw, true);
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		TreePrint tp = new TreePrint(format, "", tlp, tlp.headFinder(),
				new UniversalSemanticHeadFinder(tlp, true));
		tp.printTree(t, out);
		out.flush();
		StringBuffer sb = sw.getBuffer();
		String dependencyString = sb.toString();
		System.out.println(dependencyString);

		try {
			sw.close();
			bw.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dependencyString;
	}

	// get token index in the sentence
	public int getTokenIndex(int charIndex, boolean returnNextIfBoundary) {
		// Find:
		// The token with the highest start that is below the given character
		// index
		// The token with the lowest end that is above the given index
		List<Token> tokens = getTokens();
		int startToken = -1;
		int endToken = -1;
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if (token.getStartOffInSentence() <= charIndex)
				if (startToken == -1 || tokens.get(startToken)
						.getStartOffInSentence() <= token
								.getStartOffInSentence())
					startToken = i;
			if (token.getEndOffInSentence() > charIndex)
				if (endToken == -1 || tokens.get(endToken)
						.getEndOffInSentence() > token.getEndOffInSentence())
					endToken = i;
		}
		if (returnNextIfBoundary)
			return startToken;
		else
			return endToken;
	}

	// in BC5 annotation, sometimes, a mention may be just a part of a token
	// return the first token index of the mention
	public int getMentionStartTokenIndex(int mentionStartCharOffsetInSentence) {
		List<Token> tokens = getTokens();
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			// if (token.getStartOffInSentence() == charOffsetInSentence)
			// return i;

			if (token
					.getStartOffInSentence() <= mentionStartCharOffsetInSentence
					&& token.getEndOffInSentence() > mentionStartCharOffsetInSentence)
				return i;
		}
		return -1;
	}

	// in BC5 annotation, sometimes, a mention may be just a part of a token
	// return the last token index of the mention (inclusive)
	public int getMentionLastTokenIndex(int mentionEndCharOffsetInSentence) {
		List<Token> tokens = getTokens();
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			// if (token.getEndOffInSentence() == charOffsetInSentence)
			// return i;

			if (token.getStartOffInSentence() < mentionEndCharOffsetInSentence
					&& token.getEndOffInSentence() >= mentionEndCharOffsetInSentence)
				return i;
		}
		return -1;
	}

	// get corresponding text according to the charOffset
	public String getText(int startOff, int endOff) {
		return text.substring(startOff, endOff);
	}

	// public boolean isContainsMention(Mention men) {
	// String txt = getText(men.getStart(), men.getEnd());
	//
	// if (txt.equals(men.getText()))
	// return true;
	// return false;
	// }

	public boolean isContainsText(int startOffInDoc, int endOffInDoc,
			String text) {
		if (sentStartOffset <= startOffInDoc
				&& sentStartOffset + this.text.length() >= endOffInDoc) {
			String txt = getText(startOffInDoc - sentStartOffset,
					endOffInDoc - sentStartOffset);
			// System.out.println(txt);
			if (txt.equals(text))
				return true;
		}

		return false;
	}

	/**
	 * @return The {@link List} of {@link Mention}s for this {@link Sentence}.
	 *         This list may or may not contain overlaps
	 */
	public List<Mention> getMentions() {
		return Collections.unmodifiableList(new ArrayList<Mention>(mentions));
	}

	// public enum OverlapOption {
	// Exception, Union, Intersection, LayerInsideOut, LayerOutsideIn,
	// LeftToRight, AsSet, Raw;
	// }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((documentID == null) ? 0 : documentID.hashCode());
		result = prime * result + sentStartOffset;
		result = prime * result + sentenceIndex;
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
		Sentence other = (Sentence) obj;
		if (documentID == null) {
			if (other.documentID != null)
				return false;
		} else if (!documentID.equals(other.documentID))
			return false;
		if (sentStartOffset != other.sentStartOffset)
			return false;
		if (sentenceIndex != other.sentenceIndex)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	/**
	 * @return The {@link List} of {@link Token}s for this {@link Sentence}
	 */
	public List<Token> getTokens() {
		return Collections.unmodifiableList(new ArrayList<Token>(tokens));
	}

	public void setTokens(Set<Token> tokens) {
		this.tokens = tokens;
	}

	// clear token list
	public void clearTokens() {
		this.tokens.clear();
	}

	public void setSentenceIndex(int sentenceIndex) {
		this.sentenceIndex = sentenceIndex;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "Sentence [documentID=" + documentID + ", sentenceIndex="
				+ sentenceIndex + ", text=" + text + ", sentStartOffset="
				+ sentStartOffset + "]";
	}

	// public Sentence clone() throws CloneNotSupportedException {
	// Sentence cloned = (Sentence) super.clone();
	// cloned.tokens = new ArrayList<Token>();
	// for (Token t : this.tokens) {
	// cloned.tokens.add(t.clone());
	// }
	// cloned.mentions = new ArrayList<Mention>();
	// for (Mention m : this.mentions) {
	// cloned.mentions.add(m.clone());
	// }
	//
	// return cloned;
	// }

	@Override
	public int compareTo(Sentence o) {
		Integer compare = this.documentID.compareTo(o.documentID);
		if (compare != 0)
			return compare;

		compare = this.sentenceIndex - o.sentenceIndex;

		return compare;
	}

	public String getMentionConcealedText() {
		// Map<String, Mention> map = new TreeMap<String, Mention>();
		// List<Mention> mentions = this.getMentions();
		// for (Mention m : mentions) {
		// String concealString = "Doc_" + m.getDocumentID() + "_"
		// + m.getStartOffsetInDocument() + "_"
		// + m.getEndOffsetInDocument();
		// if (!map.containsKey(concealString))
		// map.put(concealString, m);
		// }
		// // List<Mention> mentionPool = new ArrayList<Mention>();
		// int lastCharPositionInSent = 0;// 在句子中的位移，而不在全文的位移
		// String text = this.getText();
		// StringBuilder stb = new StringBuilder();
		// for (Map.Entry<String, Mention> entry : map.entrySet()) {
		// String concealStr = entry.getKey();
		// Mention m = entry.getValue();
		// int mentionStartOffInSent = m.getStartOffsetInSentence(this);//
		// // 在句子中的位移，而不在全文的位移
		//
		// int mentionEndOffInSent = m.getEndOffsetInSentence(this);//
		// // 在句子中的位移，而不在全文的位移
		//
		// // mentionPool中的mention可能有交叉，现在先不处理，不过BC5中的mention在统计时并没有overlap的
		// if (lastCharPositionInSent > mentionStartOffInSent) {
		// continue;
		// }
		//
		// String substr = text.substring(lastCharPositionInSent,
		// mentionStartOffInSent);
		// stb.append(substr);
		// stb.append(concealStr);
		// lastCharPositionInSent = mentionEndOffInSent;
		// }

		List<Mention> mentions = this.getMentions();
		Set<Mention> mention_set = new TreeSet<Mention>(mentions);

		int lastCharOffInSentence = 0;
		String text = this.getText();
		StringBuilder stb = new StringBuilder();
		for (Mention men : mention_set) {
			int mentionStartOffInSent = men.getStartOffsetInSentence(this);
			int mentionEndOffInSent = men.getEndOffsetInSentence(this);

			// omit the overlappings
			if (lastCharOffInSentence > mentionStartOffInSent) {
				continue;
			}

			String substr = text.substring(lastCharOffInSentence,
					mentionStartOffInSent);
			stb.append(substr);
			stb.append(men.getMentionAnonymousText());
			lastCharOffInSentence = mentionEndOffInSent;
		}

		if (lastCharOffInSentence < text.length()) {
			String substr = text.substring(lastCharOffInSentence);
			stb.append(substr);
		}

		String mentionConcealedString = stb.toString();
		// Pattern pattern = Pattern.compile("\\(\\d+?\\)");// 过滤文本中(12)
		// Matcher matcher = pattern.matcher(mentionConcealedString);
		// matcher.find();
		// String replaceDigits = matcher.replaceAll("");

		Pattern ptn = Pattern.compile("^[A-Z]+\\: ");// truncate section block
														// tag
		Matcher matcher = ptn.matcher(mentionConcealedString);
		if (matcher.find())
			mentionConcealedString = matcher.replaceFirst("");
		return mentionConcealedString;
	}

	// for stanford pipeline
	public Annotation getStanfordSentenceAnnotation() {
		String concealedText = this.getMentionConcealedText();
		// PennTreebankTokenizer ptt = new PennTreebankTokenizer(new
		// StringReader(concealedText));
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(
				new StringReader(concealedText), new CoreLabelTokenFactory(),
				"");
		List<CoreLabel> words = new ArrayList<CoreLabel>();
		while (ptbt.hasNext()) {
			CoreLabel label = ptbt.next();
			words.add(label);
		}

		Annotation sentenceAnnotation = new Annotation(concealedText);
		sentenceAnnotation.set(CoreAnnotations.TokensAnnotation.class, words);

		// edu.stanford.nlp.pipeline.ParserAnnotatorUtils.class
		// make sure all tree nodes are CoreLabels
		// TODO: why isn't this always true? something fishy is going on
		Tree tree = Tree.valueOf(this.syntacticParsing);
		Trees.convertToCoreLabels(tree);

		// index nodes, i.e., add start and end token positions to all nodes
		// this is needed by other annotators down stream, e.g., the
		// NFLAnnotator
		tree.indexSpans(0);

		sentenceAnnotation.set(TreeCoreAnnotations.TreeAnnotation.class, tree);

		return sentenceAnnotation;
	}

	public Tree getTreeFromSyntacticParsing() {
		String synParsing = getSyntacticParsing();
		BufferedReader br = new BufferedReader(new StringReader(synParsing));
		PennTreeReader ptr = new PennTreeReader(br,
				new LabeledScoredTreeFactory(),
				new NPTmpRetainingTreeNormalizer());

		Tree t = null;
		try {
			t = ptr.readTree();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return t;
	}

	public SemanticGraph getSemanticGraphFromSyntacticParsing() {
		Tree t = getTreeFromSyntacticParsing();
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		GrammaticalStructure gs = gsf.newGrammaticalStructure(t);

		SemanticGraph graph = new SemanticGraph(
				gs.typedDependenciesCollapsed());
		// graph.prettyPrint();

		return graph;
	}

	// 抽取chemical和disease之间的句法路径
	public List<String> getSyntacticPathFromChemicalMention2DiseaseMention(
			Mention chem, Mention dis) {
		String chemEntityTypeName = chem.getEntityType().getTypeName();
		String disEntityTypeName = dis.getEntityType().getTypeName();
		if (!chemEntityTypeName.equalsIgnoreCase("Chemical")
				|| !disEntityTypeName.equalsIgnoreCase("Disease")) {
			throw new IllegalArgumentException(
					"The mention type of parameter is wrong!");
		}
		if (!this.documentID.equals(chem.getDocumentID())
				|| !this.documentID.equals(dis.getDocumentID())) {
			throw new IllegalArgumentException(
					"The documentID of the given mention is wrong!");
		}
		if (this.sentenceIndex != chem.getSentenceIndex()
				|| this.sentenceIndex != dis.getSentenceIndex()) {
			throw new IllegalArgumentException(
					"The sentence index of the given mention is wrong!");
		}

		// Tree tree = Tree.valueOf(this.syntacticParsing);
		Tree tree = getTreeFromSyntacticParsing();
		// tree.pennPrint();

		String chemConcealedString = chem.getMentionAnonymousText();
		String disConcealedString = dis.getMentionAnonymousText();

		List<Tree> treeNodeList = tree.preOrderNodeList();
		Tree chemTreeNode = null;
		Tree disTreeNode = null;
		for (Tree t : treeNodeList) {
			if (t.nodeString().indexOf(chemConcealedString) != -1) {
				chemTreeNode = t;
				break;
			}
		}

		for (Tree t : treeNodeList) {
			if (t.nodeString().indexOf(disConcealedString) != -1) {
				disTreeNode = t;
				break;
			}
		}

		List<Tree> pathBetweenNodes = tree.pathNodeToNode(chemTreeNode,
				disTreeNode);
		List<String> path = new ArrayList<String>();

		if (pathBetweenNodes != null && !pathBetweenNodes.isEmpty()) {
			for (int i = 1; i < pathBetweenNodes.size() - 1; i++)
				path.add(pathBetweenNodes.get(i).label().toString());
		}

		System.out.println(path);
		return Collections.unmodifiableList(path);
	}

	public List<String> getSyntacticPathFromDiseaseMention2ChemicalMention(
			Mention dis, Mention chem) {
		List<String> pathFromChem2Dis = getSyntacticPathFromChemicalMention2DiseaseMention(
				chem, dis);
		List<String> path = new ArrayList<String>();
		for (int i = pathFromChem2Dis.size() - 1; i >= 0; i--)
			path.add(pathFromChem2Dis.get(i));

		System.out.println(path);
		return Collections.unmodifiableList(path);
	}

	@Deprecated
	public List<String> getShortestDirectedPathEdgesFromChemical2Disease(
			Mention chem, Mention dis) {
		String chemEntityTypeName = chem.getEntityType().getTypeName();
		String disEntityTypeName = dis.getEntityType().getTypeName();
		if (!chemEntityTypeName.equalsIgnoreCase("Chemical")
				|| !disEntityTypeName.equalsIgnoreCase("Disease")) {
			throw new IllegalArgumentException(
					"The mention type of parameter is wrong!");
		}
		if (!this.documentID.equals(chem.getDocumentID())
				|| !this.documentID.equals(dis.getDocumentID())) {
			throw new IllegalArgumentException(
					"The documentID of the given mention is wrong!");
		}
		if (this.sentenceIndex != chem.getSentenceIndex()
				|| this.sentenceIndex != dis.getSentenceIndex()) {
			throw new IllegalArgumentException(
					"The sentence index of the given mention is wrong!");
		}

		SemanticGraph graph = this.getSemanticGraphFromSyntacticParsing();
		graph.prettyPrint();

		String chemConcealedString = chem.getMentionAnonymousText();

		String disConcealedString = dis.getMentionAnonymousText();

		Set<IndexedWord> indexedWords = graph.vertexSet();
		IndexedWord chemIndexedWord = null;
		IndexedWord disIndexedWord = null;
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(chemConcealedString) != -1) {
				chemIndexedWord = w;
				break;
			}
		}
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(disConcealedString) != -1) {
				disIndexedWord = w;
				break;
			}
		}

		List<SemanticGraphEdge> graphPath = graph
				.getShortestUndirectedPathEdges(chemIndexedWord,
						disIndexedWord);
		// List<IndexedWord> nodePath = graph.getShortestDirectedPathNodes(
		// chemIndexedWord, disIndexedWord);

		List<String> path = new ArrayList<String>();
		if (graphPath != null && !graphPath.isEmpty()) {
			for (SemanticGraphEdge eg : graphPath) {
				path.add(eg.getRelation().toString());
			}
		}

		return Collections.unmodifiableList(path);
	}

	@Deprecated
	public List<String> getShortestDirectedPathEdgesFromDisease2Chemical(
			Mention dis, Mention chem) {
		String chemEntityTypeName = chem.getEntityType().getTypeName();
		String disEntityTypeName = dis.getEntityType().getTypeName();
		if (!chemEntityTypeName.equalsIgnoreCase("Chemical")
				|| !disEntityTypeName.equalsIgnoreCase("Disease")) {
			throw new IllegalArgumentException(
					"The mention type of parameter is wrong!");
		}
		if (!this.documentID.equals(chem.getDocumentID())
				|| !this.documentID.equals(dis.getDocumentID())) {
			throw new IllegalArgumentException(
					"The documentID of the given mention is wrong!");
		}
		if (this.sentenceIndex != chem.getSentenceIndex()
				|| this.sentenceIndex != dis.getSentenceIndex()) {
			throw new IllegalArgumentException(
					"The sentence index of the given mention is wrong!");
		}

		SemanticGraph graph = this.getSemanticGraphFromSyntacticParsing();
		graph.prettyPrint();

		String chemConcealedString = chem.getMentionAnonymousText();

		String disConcealedString = dis.getMentionAnonymousText();

		Set<IndexedWord> indexedWords = graph.vertexSet();
		IndexedWord chemIndexedWord = null;
		IndexedWord disIndexedWord = null;
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(chemConcealedString) != -1) {
				chemIndexedWord = w;
				break;
			}
		}
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(disConcealedString) != -1) {
				disIndexedWord = w;
				break;
			}
		}
		List<SemanticGraphEdge> graphPath = graph
				.getShortestUndirectedPathEdges(disIndexedWord,
						chemIndexedWord);
		// List<IndexedWord> nodePath = graph.getShortestDirectedPathNodes(
		// disIndexedWord, chemIndexedWord);

		List<String> path = new ArrayList<String>();
		if (graphPath != null && !graphPath.isEmpty()) {
			for (SemanticGraphEdge eg : graphPath) {
				path.add(eg.getRelation().toString());
			}
		}
		return Collections.unmodifiableList(path);
	}

	public List<SemanticGraphEdge> getShortestUndirectedPathEdges(
			Mention fst_mention, Mention snd_mention) {
		if (!this.documentID.equals(fst_mention.getDocumentID())
				|| !this.documentID.equals(snd_mention.getDocumentID())) {
			throw new IllegalArgumentException(
					"The documentID of the given mention is wrong!");
		}
		if (this.sentenceIndex != fst_mention.getSentenceIndex()
				|| this.sentenceIndex != snd_mention.getSentenceIndex()) {
			throw new IllegalArgumentException(
					"The sentence index of the given mention is wrong!");
		}

		SemanticGraph graph = this.getSemanticGraphFromSyntacticParsing();
		// graph.prettyPrint();
		// System.out.println(graph.toList());

		String fst_ConcealedString = fst_mention.getMentionAnonymousText();
		String snd_ConcealedString = snd_mention.getMentionAnonymousText();

		Set<IndexedWord> indexedWords = graph.vertexSet();
		IndexedWord fst_IndexedWord = null;
		IndexedWord snd_IndexedWord = null;
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(fst_ConcealedString) != -1) {
				fst_IndexedWord = w;
				break;
			}
		}
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(snd_ConcealedString) != -1) {
				snd_IndexedWord = w;
				break;
			}
		}

		List<SemanticGraphEdge> graphPath = graph
				.getShortestUndirectedPathEdges(fst_IndexedWord,
						snd_IndexedWord);

		if (graphPath == null)
			return null;
		else
			return Collections.unmodifiableList(graphPath);

	}

	public List<IndexedWord> getShortestUndirectedPathNodes(Mention fst_mention,
			Mention snd_mention) {
		if (!this.documentID.equals(fst_mention.getDocumentID())
				|| !this.documentID.equals(snd_mention.getDocumentID())) {
			throw new IllegalArgumentException(
					"The documentID of the given mention is wrong!");
		}
		if (this.sentenceIndex != fst_mention.getSentenceIndex()
				|| this.sentenceIndex != snd_mention.getSentenceIndex()) {
			throw new IllegalArgumentException(
					"The sentence index of the given mention is wrong!");
		}

		SemanticGraph graph = this.getSemanticGraphFromSyntacticParsing();
		// graph.prettyPrint();
		// System.out.println(graph.toList());

		String fst_ConcealedString = fst_mention.getMentionAnonymousText();
		String snd_ConcealedString = snd_mention.getMentionAnonymousText();

		Set<IndexedWord> indexedWords = graph.vertexSet();
		IndexedWord fst_IndexedWord = null;
		IndexedWord snd_IndexedWord = null;
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(fst_ConcealedString) != -1) {
				fst_IndexedWord = w;
				break;
			}
		}
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(snd_ConcealedString) != -1) {
				snd_IndexedWord = w;
				break;
			}
		}

		List<IndexedWord> graphPath = graph.getShortestUndirectedPathNodes(
				fst_IndexedWord, snd_IndexedWord);

		if (graphPath == null)
			return null;
		else
			return Collections.unmodifiableList(graphPath);

	}

	public IndexedWord getMentionVertexInDepGraph(Mention m) {
		String menConcealText = m.getMentionAnonymousText();
		SemanticGraph graph = getSemanticGraphFromSyntacticParsing();
		Set<IndexedWord> indexedWords = graph.vertexSet();
		IndexedWord chemIndexedWord = null;
		for (IndexedWord w : indexedWords) {
			if (w.toString().indexOf(menConcealText) != -1) {
				chemIndexedWord = w;
				break;
			}
		}

		return chemIndexedWord;
	}

	public static void main(String[] args)
			throws CloneNotSupportedException, FileNotFoundException {
		String text = "Tricuspid valve regurgitation and lithium carbonate toxicity in a newborn infant.";
		String parseTree = "(ROOT (NP (NP (JJ Doc_6794356_0_29) (CC and) (JJ Doc_6794356_34_51) (NNS Doc_6794356_52_60)) (PP (IN in) (NP (DT a) (JJ newborn) (NN infant))) (. .)))";
		Sentence s = new Sentence("6794356", 0, text, 0);
		Tokenizer tokenizer = new SimpleTokenizer();
		tokenizer.tokenize(s);
		s.setSyntacticParseTree(parseTree);
		// s.setDependencyCompactString(depCompactString);
		Mention m1 = new Mention(s.getDocumentID(), s.getSentenceIndex(), 0, 29,
				"Tricuspid valve regurgitation", new EntityType("Disease"));
		m1.setConceptID("D014262");
		Mention m2 = new Mention(s.getDocumentID(), s.getSentenceIndex(), 34,
				51, "lithium carbonate", new EntityType("Chemical"));
		m2.setConceptID("D016651");
		s.addMention(m1);
		s.addMention(m2);

		// List<Mention> mentions = s.getMentions();
		// List<Token> tokens = s.getTokens();
		// for (int i = 0; i < tokens.size(); i++) {
		// Token token = tokens.get(i);
		// System.out.println("No.: " + i);
		// System.out.println("index: " + token.getTokenIndexInSentence(s));
		// }
		// System.out.println(s.getMentionConcealedText());
		// System.out.println(s.getShortestUndirectedPathEdges(m1, m2));
		// System.out.println(s.equals(s.clone()));
		// System.out.println(s.clone());

		// TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		// SemanticGraph graph = s.getSemanticGraphFromSyntacticParsing();
		// TreePrint tp = new TreePrint("typedDependenciesCollapsed", "", tlp,
		// tlp.headFinder(), new UniversalSemanticHeadFinder(tlp, true));
		// tp.printTree(s.getTreeFromSyntacticParsing());
		// IndexedWord chemIndexedWord = s.getMentionVertexInDepGraph(m1);
		//
		// IndexedWord root = graph.getFirstRoot();
		// List<SemanticGraphEdge> chem_path_to_root = graph
		// .getShortestDirectedPathEdges(root, chemIndexedWord);
		// // List<IndexedWord> chem_path_to_root = graph
		// // .getPathToRoot(chemIndexedWord);
		// System.out.println(chem_path_to_root);

		/**********/
		// String synParsing = "(ROOT (S (NP (NP (NNS Patients)) (PP (IN with)
		// (NP (NNS Doc_10365197_597_601)))) (VP (VBD had) (NP (NP (NN symptom)
		// (NN severity) (NNS levels)) (PP (IN between) (NP (NP (DT those)) (PP
		// (IN of) (NP (NNS patients)))))) (PP (IN with) (CC and) (IN without)
		// (NP (DT a) (NNS Doc_10365197_675_688)))) (. .)))";
		String synParsing = "(S1 (NP (NP (CD Doc_10728962_0_11)) (PP (IN in) (NP (NP (DT the) (NN treatment)) (PP (IN of) (NP (NP (NNP Doc_10728962_32_41-induced) (NNP Doc_10728962_50_61)) (PP (IN in) (NP (JJ severe) (CD Doc_10728962_72_85))))))) (. .)))";
		BufferedReader br = new BufferedReader(new StringReader(synParsing));
		PennTreeReader ptr = new PennTreeReader(br,
				new LabeledScoredTreeFactory(),
				new NPTmpRetainingTreeNormalizer());

		Tree t = null;
		try {
			t = ptr.readTree();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// StringWriter sw = new StringWriter();
		// BufferedWriter bw = new BufferedWriter(sw);
		// PrintWriter out = new PrintWriter(bw, true);
		// // TreePrint tp = new TreePrint("typedDependenciesCollapsed");
		// TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		// TreePrint tp = new TreePrint("typedDependenciesCollapsed", "", tlp,
		// tlp.headFinder(), new UniversalSemanticHeadFinder(tlp, true));
		// tp.printTree(t, out);
		// out.flush();
		// StringBuffer sb = sw.getBuffer();
		// String sss = sb.toString();
		// System.out.println(sss);

		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		GrammaticalStructure gs = gsf.newGrammaticalStructure(t);
		TreePrint tp = new TreePrint("typedDependenciesCollapsed", "", tlp,
				tlp.headFinder(), new UniversalSemanticHeadFinder(tlp, true));
		tp.printTree(t);

		SemanticGraph graph = new SemanticGraph(
				gs.typedDependenciesCollapsed());
		Set<IndexedWord> indexedWords = graph.vertexSet();
		IndexedWord word = null;
		IndexedWord root = graph.getFirstRoot();
		for (IndexedWord w : indexedWords) {
			if (w.word().indexOf("severe") != -1)
				word = w;
		}
		System.out.println(graph.getShortestDirectedPathNodes(word, root));

		System.out.println(root.lemma());
		// graph.prettyPrint();
		// System.out.println(graph.getFirstRoot());
		/**********/

		System.out.println("\n**** End ****\n");
	}

}
