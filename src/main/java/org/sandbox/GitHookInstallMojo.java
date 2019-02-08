package org.sandbox;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Mojo(name = "install", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public final class GitHookInstallMojo extends AbstractMojo {

    private static final String SHEBANG = "#!/bin/sh";

    @Parameter
    private Map<String, String> hooks;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private String buildDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path hooksDir = getHooksDir(buildDirectory);

        if (hooksDir == null) {
            throw new MojoExecutionException(
                    String.format(
                            "Not a git repository, could not find a .git/hooks directory anywhere in the hierarchy of %s",
                            buildDirectory));
        }

        for (Map.Entry<String, String> hook : hooks.entrySet()) {
            String hookName = hook.getKey();
            String finalScript = SHEBANG + '\n' + hook.getValue();
            try {
                getLog().debug(String.format("Installing %s hook into %s", hookName,
                        hooksDir.toAbsolutePath().toString()));
                writeFile(hooksDir.resolve(hookName), finalScript.getBytes());
            } catch (IOException e) {
                throw new MojoExecutionException("Could not write hook with name: " + hookName, e);
            }
        }
    }

    protected synchronized void writeFile(Path path, byte[] bytes) throws IOException {
        File created = Files.write(path, bytes, CREATE, TRUNCATE_EXISTING).toFile();
        boolean success = created.setExecutable(true, true)
                && created.setReadable(true, true)
                && created.setWritable(true, true);
        if (!success) {
            throw new IllegalStateException(
                    String.format("Could not set permissions on created file %s",
                            created.getAbsolutePath()));
        }
    }

    private Path getHooksDir(String base) {
        getLog().debug(String.format("Searching for .git directory starting at %s", base));
        File gitMetadataDir = new FileRepositoryBuilder()
                .findGitDir(new File(base))
                .getGitDir();

        return gitMetadataDir == null ? null : gitMetadataDir.toPath().resolve("hooks");
    }

}
