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

@Mojo(name = "install", defaultPhase = LifecyclePhase.INITIALIZE)
public final class GitHookInstallMojo extends AbstractMojo {

    private static final String SHEBANG = "#!/bin/sh";

    @Parameter
    private Map<String, String> hooks;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path hooksDir;
        try {
            hooksDir = getHooksDir();
        } catch (IOException e) {
            throw new MojoExecutionException("Exception searching for .git/hooks", e);
        }

        if (hooksDir == null) {
            throw new MojoExecutionException(
                    "Not a git repository, could not find .git/hooks directory");
        }

        for (Map.Entry<String, String> hook : hooks.entrySet()) {
            String hookName = hook.getKey();
            String finalScript = SHEBANG + '\n' + hook.getValue();
            try {
                File created = Files.write(hooksDir.resolve(hookName), finalScript.getBytes(),
                        CREATE, TRUNCATE_EXISTING).toFile();
                boolean successul = created.setExecutable(true, true)
                        && created.setReadable(true, true)
                        && created.setWritable(true, true);
                if (!successul) {
                    throw new IllegalStateException(
                            String.format("Could not set permissions on created file %s",
                                    created.getAbsolutePath()));
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Could not write hook with name: " + hookName, e);
            }
        }
    }

    private Path getHooksDir() throws IOException {
        File gitMetadataDir = new FileRepositoryBuilder()
                .findGitDir()
                .build()
                .getDirectory();

        return gitMetadataDir == null ? null : gitMetadataDir.toPath().resolve("hooks");
    }

}
