package gjh.bc5.dataset;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Stack;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

//sentence breaker using stanford tools including some simple post processing
public class SentenceBreakerWithStanford {

	private List<String> sentences;
	private List<Integer> sentenceStartOffset;
	private String title;
	private String abText;

	public SentenceBreakerWithStanford() {
		title = "";
		abText = "";
		sentences = new ArrayList<String>();
		sentenceStartOffset = new ArrayList<Integer>();
	}

	public SentenceBreakerWithStanford(String title, String abText) {
		if (title == null || title.isEmpty())
			throw new IllegalArgumentException("Invalid title: " + title);
		this.title = title;

		if (abText == null || abText.isEmpty())
			throw new IllegalArgumentException("Invalid abstract: " + abText);
		this.abText = abText;

		sentences = new ArrayList<String>();
		sentenceStartOffset = new ArrayList<Integer>();
	}

	@Deprecated
	public void segementSentences() {
		Queue<String> queue = new ArrayDeque<String>();
		Stack<String> stack = new Stack<String>();

		String totalText = title + " " + abText;
		String tmp = title + " ";

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation unSegmentedSentences = null;
		unSegmentedSentences = new Annotation(abText); // stanford segment
		pipeline.annotate(unSegmentedSentences);
		List<CoreMap> stanfordSentences = unSegmentedSentences
				.get(SentencesAnnotation.class);
		for (CoreMap sent : stanfordSentences) {
			queue.add(sent.toString().trim());
		}

		// post processing
		stack.push(title + " ");
		while (!queue.isEmpty()) {
			String current = queue.poll();
			// System.out.println(current);
			while (totalText.substring(tmp.length(), tmp.length() + 1)
					.equals(" ")) {
				tmp += " ";
				String previous = stack.pop();
				stack.push(previous + " ");
			}

			if (isBeginWithUpperCaseLetter(current)) {
				String previous = stack.pop();
				sentences.add(previous.trim());
				stack.push(current);
				tmp += current;
			} else {
				String previous = stack.pop();
				stack.push(previous + current);
				tmp += current;
			}
		}

		if (!stack.isEmpty()) {
			String last = stack.pop();
			sentences.add(last.trim());
			tmp += last;
		}

		// find the offset of the sentence in the original document
		String ttt = totalText;
		int begin = 0;
		for (int i = 0; i < sentences.size(); i++) {

			int sentStartOff = ttt.indexOf(sentences.get(i));

			sentenceStartOffset.add(begin + sentStartOff);
			begin += sentStartOff;
			begin += sentences.get(i).length();
			ttt = totalText.substring(begin);
		}
	}

	public void segementSentencesWithPostProcessing() {
		Queue<String> queue = new ArrayDeque<String>();
		Stack<String> stack = new Stack<String>();

		String totalText = title + " " + abText;
		String tmp = "";

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation unSegmentedTitle = null;
		Annotation unSegmentedAbstract = null;
		unSegmentedTitle = new Annotation(title); // stanford segment
		unSegmentedAbstract = new Annotation(abText); // stanford segment
		pipeline.annotate(unSegmentedTitle);
		List<CoreMap> stanfordTitleSentences = unSegmentedTitle
				.get(SentencesAnnotation.class);
		for (CoreMap sent : stanfordTitleSentences) {
			queue.add(sent.toString().trim());
		}

		pipeline.annotate(unSegmentedAbstract);
		List<CoreMap> stanfordAbstractSentences = unSegmentedAbstract
				.get(SentencesAnnotation.class);
		for (CoreMap sent : stanfordAbstractSentences) {
			queue.add(sent.toString().trim());
		}

		// post processing
		stack.push("");
		while (!queue.isEmpty()) {
			String current = queue.poll();
			// System.out.println(current);
			while (totalText.substring(tmp.length(), tmp.length() + 1)
					.equals(" ")) {
				tmp += " ";
				String previous = stack.pop();
				stack.push(previous + " ");
			}

			if (isBeginWithUpperCaseLetter(current)) {
				String previous = stack.pop();
				if (!previous.isEmpty())
					sentences.add(previous.trim());
				stack.push(current);
				tmp += current;
			} else if (isBeginWithLowerCaseLetterFollowedByUpperCaseLetter(
					current)) {
				String previous = stack.pop();
				if (!previous.isEmpty())
					sentences.add(previous.trim());
				stack.push(current);
				tmp += current;
			} else {
				String previous = stack.pop();
				stack.push(previous + current);
				tmp += current;
			}
		}

		if (!stack.isEmpty()) {
			String last = stack.pop();
			sentences.add(last.trim());
			tmp += last;
		}

		// find the offset of the sentence in the original document
		String ttt = totalText;
		int begin = 0;
		for (int i = 0; i < sentences.size(); i++) {

			int sentStartOff = ttt.indexOf(sentences.get(i));

			sentenceStartOffset.add(begin + sentStartOff);
			begin += sentStartOff;
			begin += sentences.get(i).length();
			ttt = totalText.substring(begin);
		}
	}

	boolean isBeginWithUpperCaseLetter(String s) {
		if (s != null && !s.isEmpty()) {
			char ch = s.charAt(0);
			if (ch >= 'A' && ch <= 'Z')
				return true;
		}
		return false;
	}

	// some special names need be taken carefully, such as rRNA, rTMS
	boolean isBeginWithLowerCaseLetterFollowedByUpperCaseLetter(String s) {
		if (s != null && !s.isEmpty() && s.length() >= 2) {
			char ch = s.charAt(0);
			char secChar = s.charAt(1);
			if (ch >= 'a' && ch <= 'z') {
				if (secChar >= 'A' && secChar <= 'Z') {
					return true;
				}
			}
		}
		return false;
	}

	public String getTitle() {
		return title;
	}

	private void setTitle(String title) {
		this.title = title;
	}

	public String getAbText() {
		return abText;
	}

	private void setAbText(String abText) {
		this.abText = abText;
	}

	public void setText(String title_text, String abstract_text) {
		sentences.clear();
		sentenceStartOffset.clear();
		setTitle(title_text);
		setAbText(abstract_text);
	}

	public List<String> getSentences() {
		return sentences;
	}

	public List<Integer> getSentenceStartOffset() {
		return sentenceStartOffset;
	}

	public static void main(String[] args) {
		String title = "Little information is available to predict which patients are at highest risk for this complication. OBJECTIVE: To quantify specific clinical predictors of reduction in renal function in patients with CHF who are prescribed angiotensin-converting enzyme inhibitor therapy. Predictors of decreased renal function in patients with heart failure during angiotensin-converting enzyme inhibitor therapy: results from the studies of left ventricular dysfunction (SOLVD)";
		String abText = "BACKGROUND: Although angiotensin-converting enzyme inhibitor therapy reduces mortality rates in patients with congestive heart failure (CHF), it may also cause decreased renal function. Ten rats received saline as a control group. cTnI was measured with Access(R) (ng/ml) and a research immunoassay (pg/ml), and compared with cTnT, CK-MB mass and CK.";
		SentenceBreakerWithStanford sd = new SentenceBreakerWithStanford(title,
				abText);
		// sd.setTitle("Acute changes of blood ammonia may predict short-term
		// adverse effects of valproic acid.");
		// sd.setAbText("Valproic acid (VPA) was given to 24 epileptic patients
		// who were already being treated with other antiepileptic drugs. A
		// standardized loading dose of VPA was administered, and venous blood
		// was sampled at 0, 1, 2, 3, and 4 hours. Ammonia (NH3) was higher in
		// patients who, during continuous therapy, complained of drowsiness (7
		// patients) than in those who were symptom-free (17 patients), although
		// VPA plasma levels were similar in both groups. By measuring
		// VPA-induced changes of blood NH3 content, it may be possible to
		// identify patients at higher risk of obtundation when VPA is given
		// chronically.");

		sd.segementSentencesWithPostProcessing();
		List<String> sentences = sd.getSentences();
		List<Integer> sentOff = sd.getSentenceStartOffset();
		for (String s : sentences)
			System.out.println(s);
		for (Integer s : sentOff)
			System.out.println(s);

		System.out.println();

		sd.setText(
				"Indomethacin induced hypotension in sodium and volume depleted rats.",
				"After a single oral dose of 4 mg/kg indomethacin (IDM) to sodium and volume depleted rats plasma renin activity (PRA) and systolic blood pressure fell significantly within four hours. In sodium repleted animals indomethacin did not change systolic blood pressure (BP) although plasma renin activity was decreased. Thus, indomethacin by inhibition of prostaglandin synthesis may diminish the blood pressure maintaining effect of the stimulated renin-angiotensin system in sodium and volume depletion.");
		sd.segementSentencesWithPostProcessing();
		sentences = sd.getSentences();
		sentOff = sd.getSentenceStartOffset();
		for (String s : sentences)
			System.out.println(s);
		for (Integer s : sentOff)
			System.out.println(s);
	}

}
