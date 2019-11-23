/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * User: mike Date: 11/22/19 Time: 22:43
 */
public class FileLister  extends SimpleFileVisitor<Path> {
  private final List<String> files;
  private final Set<String> excludes;
  private final String subdir;

  public FileLister(List<String> files,
                    final Set<String> excludes,
                    final String subdir) {
    this.files = files;
    this.excludes = excludes;
    this.subdir = subdir;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir,
                                           BasicFileAttributes attrs) {
    Objects.requireNonNull(dir);
    Objects.requireNonNull(attrs);

    if ((subdir == null) || (subdir.equals(dir.toString()))) {
      return FileVisitResult.CONTINUE;
    }

    return FileVisitResult.SKIP_SUBTREE;
  }

  @Override
  public FileVisitResult visitFile(Path file,
                                   BasicFileAttributes attr) {
    if (!attr.isRegularFile()) {
      return CONTINUE;
    }

    var nm = file.getFileName().toString();

    if (!nm.endsWith(".xml") || excludes.contains(nm)) {
      return CONTINUE;
    }

    files.add(nm);
    return CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file,
                                         IOException exc) {
    System.err.println(exc);
    return CONTINUE;
  }
}
