package net.unfamily.anotherstages.recipe;

/** Stage gate condition shared by recipe/block rules. */
public final class StageCondition {
    public String stageType = "player";
    public String stage = "";
    /** Lock while stage is missing when false (default); lock while present when true. */
    public boolean is = false;
}
