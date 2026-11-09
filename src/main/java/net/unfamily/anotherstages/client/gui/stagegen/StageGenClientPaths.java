package net.unfamily.anotherstages.client.gui.stagegen;

/**
 * Client cache of the first Library datapack path (pushed when the generator opens).
 */
public final class StageGenClientPaths {
    private static String firstDatapackPath = "";

    private StageGenClientPaths() {}

    public static void setFirstDatapackPath(String path) {
        firstDatapackPath = path == null ? "" : path;
    }

    public static String firstDatapackPath() {
        return firstDatapackPath;
    }
}
