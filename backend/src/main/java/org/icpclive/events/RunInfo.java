package org.icpclive.events;

public interface RunInfo {
    int getId();
    boolean isAccepted();
    boolean isAddingPenalty();
    boolean isJudged();
    String getResult();
    ProblemInfo getProblem();
    int getProblemId();
    int getTeamId();
    boolean isReallyUnknown();
    double getPercentage();

    long getTime();
    long getLastUpdateTime();

    default boolean isFirstSolvedRun() {
        return EventsLoader.getInstance().getContestData().firstSolvedRun()[getProblemId()] == this;
    }
}
