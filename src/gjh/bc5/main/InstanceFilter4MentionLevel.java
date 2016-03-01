package gjh.bc5.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gjh.bc5.features.MentionPairAsInstanceSource;
import gjh.bc5.utils.Abstract;
import gjh.bc5.utils.Mention;
import gjh.bc5.utils.Relation;
import gjh.bc5.utils.Sentence;
import gjh.bc5.utils.Token;

public class InstanceFilter4MentionLevel {

	private static final int PARAMETER_MAXIMUM_TOKEN_DISTANCE_BETWEEN_2_MENTIONS = 10;

	public static List<MentionPairAsInstanceSource> filterMentionLevelInstancesOfTrainingCooccurrence(
			List<Relation> original_relations) {
		boolean onlyFilterPositive = false;

		if (onlyFilterPositive) {
			List<MentionPairAsInstanceSource> mpi = new ArrayList<MentionPairAsInstanceSource>();
			List<MentionPairAsInstanceSource> mpiPositives = new ArrayList<MentionPairAsInstanceSource>();
			List<MentionPairAsInstanceSource> mpiNegatives = new ArrayList<MentionPairAsInstanceSource>();
			for (Relation r : original_relations) {
				if (r.getRelationType().equalsIgnoreCase("CID")) {// positive
					if (!r.hasCooccurrenceInOneSentence())
						continue;
					List<MentionPairAsInstanceSource> tmp = getCooccurMentionPairAfterFilteringStrategies(
							r);
					if (tmp != null && !tmp.isEmpty()) {
						mpiPositives.addAll(tmp);
					}
				} else {// negative
					if (r.hasCooccurrenceInOneSentence()) {
						List<Mention> cooccurChemMentions = new ArrayList<Mention>();
						List<Mention> cooccurDisMentions = new ArrayList<Mention>();
						r.getCoocurrenceMentionPairsInOneSentence(
								cooccurChemMentions, cooccurDisMentions);
						for (int i = 0; i < cooccurChemMentions.size(); i++) {
							MentionPairAsInstanceSource instanceSource = new MentionPairAsInstanceSource(
									cooccurChemMentions.get(i),
									cooccurDisMentions.get(i), r);
							mpiNegatives.add(instanceSource);
						}

					}
				}
			}
			mpi.addAll(mpiPositives);
			mpi.addAll(mpiNegatives);

			return mpi;
		} else {
			List<MentionPairAsInstanceSource> mpi = new ArrayList<MentionPairAsInstanceSource>();
			for (Relation r : original_relations) {
				if (!r.hasCooccurrenceInOneSentence())
					continue;

				List<MentionPairAsInstanceSource> tmp = getCooccurMentionPairAfterFilteringStrategies(
						r);
				if (tmp != null && !tmp.isEmpty()) {
					mpi.addAll(tmp);
				}
			}
			return mpi;
		}
	}

	// whether the char is a sub sentence delimiter, such as ";:,"
	private static boolean isSubSentenceDelimiter(char ch) {
		// ,;:
		return (",;:".indexOf(ch) != -1);
	}

	private static boolean isSubSentenceDelimiter(String str) {
		if (str.length() == 1) {
			char ch = str.charAt(0);
			return isSubSentenceDelimiter(ch);
		} else {
			return false;
		}
	}

	private static boolean hasSubSentenceDelimiterBetweenMentionPair(
			MentionPairAsInstanceSource mpi) {
		Mention chem = mpi.getChemMention();
		Mention dis = mpi.getDisMention();
		Relation r = mpi.getRelationSource();
		List<Token> tokensInBetween = r.getTokensBetweenTwoMentions(chem, dis);
		for (Token t : tokensInBetween) {
			String tokenText = t.getText();
			if (isSubSentenceDelimiter(tokenText)) {
				return true;
			}
		}
		return false;
	}

	private static List<MentionPairAsInstanceSource> selectCooccurMentionPairsByStrategy_WhetherInSameSubSentence(
			List<MentionPairAsInstanceSource> inMentionPairs) {
		if (inMentionPairs == null)
			throw new IllegalArgumentException(
					"The input list of mention pairs is not ready!");

		List<MentionPairAsInstanceSource> returnMentionPairs = new ArrayList<MentionPairAsInstanceSource>();
		for (MentionPairAsInstanceSource mp : inMentionPairs) {
			if (!hasSubSentenceDelimiterBetweenMentionPair(mp))
				returnMentionPairs.add(mp);
		}

		if (returnMentionPairs.isEmpty()) {
			return inMentionPairs;
		}

		return returnMentionPairs;
	}

	private static boolean isMentionSurroundedByBracket(Abstract a, Mention m) {
		if (!a.getDocumentID().equals(m.getDocumentID())) {
			throw new IllegalArgumentException("docid does not equals!");
		}

		Sentence s = m.getSentence(a);
		List<Token> tokens = s.getTokens();
		int tokenStartIndex = m.getStartTokenIndexInSentence(s);
		String leftMark = "";
		for (int i = tokenStartIndex; i >= 0; i--) {
			String wordText = tokens.get(i).getText();
			if (wordText.equals("(") || wordText.equals(")")) {
				leftMark = wordText;
				break;
			}
		}

		if (leftMark.isEmpty() || leftMark.equals(")")) {
			return false;
		} else {
			return true;
		}
	}

	public static List<MentionPairAsInstanceSource> getCooccurMentionPairAfterFilteringStrategies(
			Relation r) {
		if (!r.hasCooccurrenceInOneSentence()) {
			return new ArrayList<MentionPairAsInstanceSource>();
		}

		List<Mention> cooccurChemMentions = new ArrayList<Mention>();
		List<Mention> cooccurDisMentions = new ArrayList<Mention>();
		r.getCoocurrenceMentionPairsInOneSentence(cooccurChemMentions,
				cooccurDisMentions);
		assert (cooccurChemMentions.size() == cooccurDisMentions.size());

		List<MentionPairAsInstanceSource> mentionPairs = new ArrayList<MentionPairAsInstanceSource>();
		for (int i = 0; i < cooccurChemMentions.size(); i++) {
			Mention chem = cooccurChemMentions.get(i);
			Mention dis = cooccurDisMentions.get(i);
			mentionPairs.add(new MentionPairAsInstanceSource(chem, dis, r));
		}

		/****** filtering strategies ********/

		List<MentionPairAsInstanceSource> mentionPair_AfterSelectionBySubSentenceDelimiter = selectCooccurMentionPairsByStrategy_WhetherInSameSubSentence(
				mentionPairs);
		if (mentionPair_AfterSelectionBySubSentenceDelimiter.isEmpty())
			mentionPair_AfterSelectionBySubSentenceDelimiter = mentionPairs;

		List<MentionPairAsInstanceSource> mentionPair_AfterSelectionByBracket = selectCooccurMentionPairsByStrategy_WhetherInBracket(
				mentionPairs);
		if (mentionPair_AfterSelectionByBracket.isEmpty())
			mentionPair_AfterSelectionByBracket = mentionPairs;

		List<MentionPairAsInstanceSource> mentionPair_AfterSelectionByTokenDistance = selectCooccurMentionPairsByStrategy_TokenDistanceInBetween(
				mentionPair_AfterSelectionBySubSentenceDelimiter,
				PARAMETER_MAXIMUM_TOKEN_DISTANCE_BETWEEN_2_MENTIONS);
		if (mentionPair_AfterSelectionByTokenDistance.isEmpty())
			mentionPair_AfterSelectionByTokenDistance = mentionPair_AfterSelectionByBracket;

		List<MentionPairAsInstanceSource> mentionPair_AfterSelectionByCoreference = selectCooccurMentionPairsByStrategy_SelectTheNearestCoreference(
				mentionPair_AfterSelectionByTokenDistance);
		if (mentionPair_AfterSelectionByCoreference.isEmpty())
			mentionPair_AfterSelectionByCoreference = mentionPair_AfterSelectionByTokenDistance;

		return mentionPair_AfterSelectionByCoreference;

	}

	private static List<MentionPairAsInstanceSource> selectCooccurMentionPairAsInstanceByShortestTokenDistance(
			Relation r) {
		if (!r.hasCooccurrenceInOneSentence()) {
			return new ArrayList<MentionPairAsInstanceSource>();
		}

		List<Mention> cooccurChemMentions = new ArrayList<Mention>();
		List<Mention> cooccurDisMentions = new ArrayList<Mention>();

		List<Integer> cooccurSentenceIndex = new ArrayList<Integer>();

		List<Integer> cooccurTokenDistance = new ArrayList<Integer>();

		r.getCoocurrenceMentionPairsInOneSentence(cooccurChemMentions,
				cooccurDisMentions);
		assert (!cooccurChemMentions.isEmpty());
		assert (!cooccurDisMentions.isEmpty());
		assert (cooccurChemMentions.size() == cooccurDisMentions.size());
		for (int i = 0; i < cooccurChemMentions.size(); i++) {
			Mention chem = cooccurChemMentions.get(i);
			Mention dis = cooccurDisMentions.get(i);
			int sentIndex = chem.getSentenceIndex();
			int distance = r.getTokensBetweenTwoMentions(chem, dis).size();

			cooccurSentenceIndex.add(sentIndex);
			cooccurTokenDistance.add(distance);
		}

		Map<Integer, List<Integer>> sentIndex_correspondingCoMenNo = new HashMap<Integer, List<Integer>>();

		Map<Integer, Integer> sentIndex_shortestDistance = new HashMap<Integer, Integer>();

		for (int i = 0; i < cooccurSentenceIndex.size(); i++) {
			int sentIndex = cooccurSentenceIndex.get(i);
			int distance = cooccurTokenDistance.get(i);

			if (!sentIndex_shortestDistance.containsKey(sentIndex)) {
				sentIndex_shortestDistance.put(sentIndex, distance);
				List<Integer> list = new ArrayList<Integer>();
				list.add(i);

				sentIndex_correspondingCoMenNo.put(sentIndex, list);
			} else {
				if (distance < sentIndex_shortestDistance.get(sentIndex)) {
					List<Integer> list = sentIndex_correspondingCoMenNo
							.get(sentIndex);
					list.clear();
					list.add(i);
					sentIndex_correspondingCoMenNo.put(sentIndex, list);
					sentIndex_shortestDistance.put(sentIndex, distance);

				} else if (distance == sentIndex_shortestDistance
						.get(sentIndex)) {
					List<Integer> list = sentIndex_correspondingCoMenNo
							.get(sentIndex);
					list.add(i);
					sentIndex_correspondingCoMenNo.put(sentIndex, list);
				} else {

				}
			}
		}

		List<Integer> selectedCoMensIndex = new ArrayList<Integer>();

		Iterator<Entry<Integer, Integer>> itr = sentIndex_shortestDistance
				.entrySet().iterator();
		Entry<Integer, Integer> initEntry = itr.next();
		int sentIndex = initEntry.getKey();
		selectedCoMensIndex
				.addAll(sentIndex_correspondingCoMenNo.get(sentIndex));
		int shortest = initEntry.getValue();

		while (itr.hasNext()) {
			Entry<Integer, Integer> entry = itr.next();
			int key = entry.getKey();
			int value = entry.getValue();
			if (value < shortest) {
				shortest = value;
				selectedCoMensIndex.clear();
				selectedCoMensIndex
						.addAll(sentIndex_correspondingCoMenNo.get(key));
			} else if (value == shortest) {
				selectedCoMensIndex
						.addAll(sentIndex_correspondingCoMenNo.get(key));
			} else {

			}
		}

		List<MentionPairAsInstanceSource> mpis = new ArrayList<MentionPairAsInstanceSource>();
		for (int i = 0; i < selectedCoMensIndex.size(); i++) {
			int coMensIndex = selectedCoMensIndex.get(i);
			Mention chem = cooccurChemMentions.get(coMensIndex);
			Mention dis = cooccurDisMentions.get(coMensIndex);
			mpis.add(new MentionPairAsInstanceSource(chem, dis, r));
		}

		return mpis;
	}

	private static List<MentionPairAsInstanceSource> selectCooccurMentionPairsByStrategy_SelectTheNearestCoreference(
			List<MentionPairAsInstanceSource> inMentionPairs) {
		if (inMentionPairs == null)
			throw new IllegalArgumentException(
					"The input list of mention pairs is not ready!");

		Map<String, List<MentionPairAsInstanceSource>> optimalMentionPairs = new HashMap<String, List<MentionPairAsInstanceSource>>();
		for (int i = 0; i < inMentionPairs.size(); i++) {
			MentionPairAsInstanceSource mp = inMentionPairs.get(i);
			String DocID = mp.getRelationSource().getDocumentID();
			String chemConceptID = mp.getChemMention().getConceptID();
			String disConceptID = mp.getDisMention().getConceptID();
			int sentenceIndex = mp.getChemMention().getSentenceIndex();
			String key = DocID + "-" + chemConceptID + "-" + disConceptID + "-"
					+ sentenceIndex;
			int distanceInBetween = mp.getTokenDistanceBetweenMentions();

			if (!optimalMentionPairs.containsKey(key)) {
				List<MentionPairAsInstanceSource> shortestMenPairs = new ArrayList<MentionPairAsInstanceSource>();
				shortestMenPairs.add(mp);
				optimalMentionPairs.put(key, shortestMenPairs);
			} else {
				List<MentionPairAsInstanceSource> shortestMenPairs = optimalMentionPairs
						.get(key);
				int existDistance = shortestMenPairs.get(0)
						.getTokenDistanceBetweenMentions();

				if (distanceInBetween < existDistance) {
					shortestMenPairs.clear();
					shortestMenPairs.add(mp);
					// optimalMentionPairs.put(key, shortestMenPairs);
				}
				if (distanceInBetween == existDistance) {
					shortestMenPairs.add(mp);
				}
			}
		}

		List<MentionPairAsInstanceSource> returnMentionPairs = new ArrayList<MentionPairAsInstanceSource>();
		for (Map.Entry<String, List<MentionPairAsInstanceSource>> m : optimalMentionPairs
				.entrySet()) {
			List<MentionPairAsInstanceSource> value = m.getValue();
			returnMentionPairs.addAll(value);
		}

		return returnMentionPairs;
	}

	private static List<MentionPairAsInstanceSource> selectUncooccurMentionPairByStrategy_SelectTheNearestAndNotCross(
			Relation r) {
		if (r.hasCooccurrenceInOneSentence()) {
			throw new IllegalArgumentException(
					"The input list of relations should not have any cooccurrence!");
		}
		Set<Mention> chemMens = r.getChemicalMentions();
		Set<Mention> disMens = r.getDiseaseMentions();

		Set<Mention> mentionSequence = new TreeSet<Mention>();
		mentionSequence.addAll(chemMens);
		mentionSequence.addAll(disMens);

		List<MentionPairAsInstanceSource> selectedMentionPairs = new ArrayList<MentionPairAsInstanceSource>();
		List<Mention> mens = new ArrayList<Mention>(mentionSequence);
		for (int i = 1; i < mens.size(); i++) {
			Mention currentMention = mens.get(i);
			String currentConceptType = currentMention.getEntityType()
					.getTypeName();
			Mention lastMention = mens.get(i - 1);
			String lastConceptType = lastMention.getEntityType().getTypeName();
			if (currentConceptType.equalsIgnoreCase("Disease")
					&& lastConceptType.equalsIgnoreCase("Chemical")) {
				selectedMentionPairs.add(new MentionPairAsInstanceSource(
						lastMention, currentMention, r));
			}
		}

		return selectedMentionPairs;
	}

	private static List<MentionPairAsInstanceSource> selectUncooccurMentionPairByStrategy_WhetherInTheSameTextBlock(
			List<MentionPairAsInstanceSource> inMenPairs) {
		List<MentionPairAsInstanceSource> afterFilterMPRs = new ArrayList<MentionPairAsInstanceSource>();
		for (MentionPairAsInstanceSource mpr : inMenPairs) {
			Abstract a = mpr.getAbstract();
			String wholeText = a.getWholeText();
			Mention chemMention = mpr.getChemMention();
			Mention disMention = mpr.getDisMention();

			Mention firstMention = null;
			Mention secondMention = null;
			if (chemMention.compareTo(disMention) < 0) {
				firstMention = chemMention;
				secondMention = disMention;
			} else {
				firstMention = disMention;
				secondMention = chemMention;
			}

			int sPos = firstMention.getEndOffsetInDocument();
			int lPos = secondMention.getStartOffsetInDocument();
			String subText = wholeText.substring(sPos, lPos);
			Pattern pattern = Pattern.compile("[A-Z]+:");
			Matcher matcher = pattern.matcher(subText);
			if (!matcher.find()) {
				afterFilterMPRs.add(mpr);
			}
		}
		return afterFilterMPRs;
	}

	private static List<MentionPairAsInstanceSource> selectUncooccurMentionPairByStrategy_NearestBeforeFirstDiseaseMention(
			Relation r) {
		if (r.hasCooccurrenceInOneSentence()) {
			throw new IllegalArgumentException(
					"The input list of relations should not have any cooccurrence!");
		}
		Set<Mention> chemMens = r.getChemicalMentions();
		Set<Mention> disMens = r.getDiseaseMentions();
		Set<Mention> unCooccurChemMentions = new TreeSet<Mention>(chemMens);
		Set<Mention> unCooccurDisMentions = new TreeSet<Mention>(disMens);

		List<Mention> chemMentions = new ArrayList<Mention>(
				unCooccurChemMentions);
		List<Mention> disMentions = new ArrayList<Mention>(
				unCooccurDisMentions);

		boolean constructMentionPairsStatus = false;
		List<MentionPairAsInstanceSource> mentionPairs = new ArrayList<MentionPairAsInstanceSource>();

		for (int i = 0; i < disMentions.size(); i++) {
			if (constructMentionPairsStatus == true) {
				break;
			}
			Mention disMention = disMentions.get(i);
			int disMentionStartOffInDoc = disMention.getStartOffsetInDocument();

			for (int j = chemMentions.size() - 1; j >= 0; j--) {
				Mention chemMention = chemMentions.get(j);
				int chemMentionEndOffInDoc = chemMention
						.getEndOffsetInDocument();

				if (chemMentionEndOffInDoc <= disMentionStartOffInDoc) {
					constructMentionPairsStatus = true;

					mentionPairs.add(new MentionPairAsInstanceSource(
							chemMention, disMention, r));

					break;
				}
			}
		}
		return mentionPairs;
	}

	private static List<MentionPairAsInstanceSource> selectCooccurMentionPairsByStrategy_WhetherInBracket(
			List<MentionPairAsInstanceSource> inMentionPairs) {
		if (inMentionPairs == null)
			throw new IllegalArgumentException(
					"The input list of mention pairs is not ready!");

		List<MentionPairAsInstanceSource> returnMentionPairs = new ArrayList<MentionPairAsInstanceSource>();
		for (MentionPairAsInstanceSource mp : inMentionPairs) {
			Relation relation = mp.getRelationSource();
			Abstract ab = relation.getAbstract();
			Mention chemMention = mp.getChemMention();
			Mention disMention = mp.getDisMention();
			if (!isMentionSurroundedByBracket(ab, chemMention)
					&& !isMentionSurroundedByBracket(ab, disMention))
				returnMentionPairs.add(mp);
		}

		return returnMentionPairs;
	}

	private static List<MentionPairAsInstanceSource> selectCooccurMentionPairsByStrategy_TokenDistanceInBetween(
			List<MentionPairAsInstanceSource> inMentionPairs,
			int token_distance_threshold) {
		if (inMentionPairs == null)
			throw new IllegalArgumentException(
					"The input list of mention pairs is not ready!");

		List<MentionPairAsInstanceSource> returnMentionPairs = new ArrayList<MentionPairAsInstanceSource>();
		for (MentionPairAsInstanceSource mp : inMentionPairs) {
			if (mp.getTokenDistanceBetweenMentions() < token_distance_threshold)
				returnMentionPairs.add(mp);
		}

		return returnMentionPairs;
	}

	public static List<MentionPairAsInstanceSource> getUncooccurMentionPairAfterFilteringStrategies(
			Relation r) {
		if (r.hasCooccurrenceInOneSentence()) {
			throw new IllegalArgumentException(
					"The input list of relations should not have any cooccurrence!");
		}
		Set<Mention> chemMens = r.getChemicalMentions();
		Set<Mention> disMens = r.getDiseaseMentions();
		Set<Mention> unCooccurChemMentions = new TreeSet<Mention>(chemMens);
		Set<Mention> unCooccurDisMentions = new TreeSet<Mention>(disMens);

		List<Mention> chemMentions = new ArrayList<Mention>(
				unCooccurChemMentions);
		List<Mention> disMentions = new ArrayList<Mention>(
				unCooccurDisMentions);

		List<MentionPairAsInstanceSource> mentionPairs = selectUncooccurMentionPairByStrategy_SelectTheNearestAndNotCross(
				r);

		if (mentionPairs.isEmpty()) {
			mentionPairs
					.add(new MentionPairAsInstanceSource(chemMentions.get(0),
							disMentions.get(disMentions.size() - 1), r));
		}

		return mentionPairs;
	}

	public static List<MentionPairAsInstanceSource> filterMentionLevelInstancesOfTrainingUncooccurrence(
			List<Relation> unCooccurrRelations) {
		boolean firstlyFilterDisease = true;
		boolean onlyFilterPositive = false;

		for (Relation r : unCooccurrRelations) {
			if (r.hasCooccurrenceInOneSentence()) {
				throw new IllegalArgumentException(
						"The input list of relations should not have any cooccurrence!");
			}
		}

		List<Relation> relationsAfterFilter = null;
		if (firstlyFilterDisease) {

			List<Relation> filterDisease_WhichHasCooccurrenceWithOtherChemical = new ArrayList<Relation>();
			for (Relation r : unCooccurrRelations) {
				Abstract ab = r.getAbstract();
				Set<Mention> disMentions = r.getDiseaseMentions();

				boolean diseaseCooccurWithOtherChemical = false;
				for (Mention m : disMentions) {
					if (diseaseCooccurWithOtherChemical == true) {
						break;
					}

					Sentence sentence = m.getSentence(ab);
					List<Mention> mens = sentence.getMentions();
					for (Mention mn : mens) {
						String otherConceptType = mn.getEntityType()
								.getTypeName();
						if (otherConceptType.equalsIgnoreCase("Chemical")) {
							diseaseCooccurWithOtherChemical = true;
							// System.out
							// .println("FINDING
							// ANOTHER........................");
							// System.out.println(mn);
							break;
						}
					}
				}

				if (diseaseCooccurWithOtherChemical == false) {
					filterDisease_WhichHasCooccurrenceWithOtherChemical.add(r);
					// System.out.println("FINDING ........................");
				}
			}

			relationsAfterFilter = filterDisease_WhichHasCooccurrenceWithOtherChemical;
		} else {
			relationsAfterFilter = unCooccurrRelations;
		}

		if (onlyFilterPositive) {
			List<MentionPairAsInstanceSource> mpi = new ArrayList<MentionPairAsInstanceSource>();
			List<MentionPairAsInstanceSource> mpiPositives = new ArrayList<MentionPairAsInstanceSource>();
			List<MentionPairAsInstanceSource> mpiNegatives = new ArrayList<MentionPairAsInstanceSource>();

			for (Relation r : relationsAfterFilter) {
				if (r.getRelationType().equalsIgnoreCase("CID")) {// positive
					List<MentionPairAsInstanceSource> tmp = getUncooccurMentionPairAfterFilteringStrategies(
							r);
					if (tmp != null && !tmp.isEmpty()) {
						mpiPositives.addAll(tmp);
					}
				} else {// negative
					List<Mention> unCooccurChemMentions = new ArrayList<Mention>(
							r.getChemicalMentions());
					List<Mention> unCooccurDisMentions = new ArrayList<Mention>(
							r.getDiseaseMentions());
					for (int i = 0; i < unCooccurChemMentions.size(); i++) {
						for (int j = 0; j < unCooccurDisMentions.size(); j++) {
							MentionPairAsInstanceSource instanceSource = new MentionPairAsInstanceSource(
									unCooccurChemMentions.get(i),
									unCooccurDisMentions.get(j), r);
							mpiNegatives.add(instanceSource);
						}
					}
				}
			}
			mpi.addAll(mpiPositives);
			mpi.addAll(mpiNegatives);

			return mpi;
		} else {
			List<MentionPairAsInstanceSource> mpi = new ArrayList<MentionPairAsInstanceSource>();
			for (Relation r : relationsAfterFilter) {
				if (r.hasCooccurrenceInOneSentence())
					continue;

				List<MentionPairAsInstanceSource> tmp = getUncooccurMentionPairAfterFilteringStrategies(
						r);
				if (tmp != null && !tmp.isEmpty()) {
					mpi.addAll(tmp);
				}
			}
			return mpi;
		}
	}

	public static List<MentionPairAsInstanceSource> filterMentionLevelInstancesOfTestingCooccurrence(
			List<Relation> relations) {

		List<MentionPairAsInstanceSource> mpi = new ArrayList<MentionPairAsInstanceSource>();
		for (Relation r : relations) {
			if (!r.hasCooccurrenceInOneSentence())
				continue;

			List<MentionPairAsInstanceSource> tmp = getCooccurMentionPairAfterFilteringStrategies(
					r);
			if (tmp != null && !tmp.isEmpty()) {
				mpi.addAll(tmp);
			}
		}
		return mpi;
	}

	public static List<MentionPairAsInstanceSource> filterMentionLevelInstancesOfTestingUncooccurrence(
			List<Relation> unCooccurrRelations) {
		boolean firstlyFilterDisease = true;

		for (Relation r : unCooccurrRelations) {
			if (r.hasCooccurrenceInOneSentence()) {
				throw new IllegalArgumentException(
						"The input list of relations should not have any cooccurrence!");
			}
		}

		List<Relation> relationsAfterFilter = null;
		if (firstlyFilterDisease) {

			List<Relation> filterDisease_WhichHasCooccurrenceWithOtherChemical = new ArrayList<Relation>();
			for (Relation r : unCooccurrRelations) {
				Abstract ab = r.getAbstract();
				String chemConceptID = r.getChemicalConceptID();
				Set<Mention> disMentions = r.getDiseaseMentions();

				boolean diseaseCooccurWithOtherChemical = false;
				for (Mention m : disMentions) {
					if (diseaseCooccurWithOtherChemical == true) {
						break;
					}

					Sentence sentence = m.getSentence(ab);
					List<Mention> mens = sentence.getMentions();
					for (Mention mn : mens) {
						String otherConceptType = mn.getEntityType()
								.getTypeName();
						String otherConceptID = mn.getConceptID();
						if (otherConceptType.equalsIgnoreCase("Chemical")
								&& !otherConceptID.equals(chemConceptID)) {
							diseaseCooccurWithOtherChemical = true;
							break;
						}
					}
				}

				if (diseaseCooccurWithOtherChemical == false) {
					filterDisease_WhichHasCooccurrenceWithOtherChemical.add(r);
				}
			}

			relationsAfterFilter = filterDisease_WhichHasCooccurrenceWithOtherChemical;
		} else {
			relationsAfterFilter = unCooccurrRelations;
		}

		List<MentionPairAsInstanceSource> mpi = new ArrayList<MentionPairAsInstanceSource>();
		for (Relation r : relationsAfterFilter) {
			if (r.hasCooccurrenceInOneSentence())
				continue;

			List<MentionPairAsInstanceSource> tmp = getUncooccurMentionPairAfterFilteringStrategies(
					r);
			if (tmp != null && !tmp.isEmpty()) {
				mpi.addAll(tmp);
			}
		}
		return mpi;
	}

}
