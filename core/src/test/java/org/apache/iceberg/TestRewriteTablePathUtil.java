/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TestRewriteTablePathUtil {

  @Test
  public void testStagingPathPreservesDirectoryStructure() {
    String sourcePrefix = "/source/table";
    String stagingDir = "/staging/";

    // Two files with same name but different paths
    String file1 = "/source/table/hash1/delete_0_0_0.parquet";
    String file2 = "/source/table/hash2/delete_0_0_0.parquet";

    String stagingPath1 = RewriteTablePathUtil.stagingPath(file1, sourcePrefix, stagingDir);
    String stagingPath2 = RewriteTablePathUtil.stagingPath(file2, sourcePrefix, stagingDir);

    // Should preserve directory structure to avoid conflicts
    assertThat(stagingPath1)
        .startsWith(stagingDir)
        .isEqualTo("/staging/hash1/delete_0_0_0.parquet")
        .isNotEqualTo(stagingPath2);
    assertThat(stagingPath2)
        .startsWith(stagingDir)
        .isEqualTo("/staging/hash2/delete_0_0_0.parquet");
  }

  @Test
  public void testRewrittenMetadataJsonKeepsSourceFileName() {
    // Example matching rewrite_table_path's rewriteVersionFile naming:
    // stagingPath(source) and newPath(source) both preserve the metadata.json basename.
    String sourcePrefix = "s3://bucket-one/warehouse/db.db/orders";
    String targetPrefix = "s3://bucket-two/warehouse/db.db/orders";
    String stagingDir = "s3://bucket-staging/orders-rewrite";
    String sourceMetadata =
        "s3://bucket-one/warehouse/db.db/orders/metadata/v3.metadata.json";

    String stagingPath =
        RewriteTablePathUtil.stagingPath(sourceMetadata, sourcePrefix, stagingDir);
    String targetPath = RewriteTablePathUtil.newPath(sourceMetadata, sourcePrefix, targetPrefix);

    assertThat(RewriteTablePathUtil.fileName(stagingPath)).isEqualTo("v3.metadata.json");
    assertThat(RewriteTablePathUtil.fileName(targetPath)).isEqualTo("v3.metadata.json");
    assertThat(RewriteTablePathUtil.fileName(stagingPath))
        .isEqualTo(RewriteTablePathUtil.fileName(sourceMetadata));
    assertThat(RewriteTablePathUtil.fileName(targetPath))
        .isEqualTo(RewriteTablePathUtil.fileName(sourceMetadata));

    assertThat(stagingPath)
        .isEqualTo("s3://bucket-staging/orders-rewrite/metadata/v3.metadata.json");
    assertThat(targetPath)
        .isEqualTo("s3://bucket-two/warehouse/db.db/orders/metadata/v3.metadata.json");

    // UUID-style metadata names are preserved the same way
    String uuidMetadata =
        "s3://bucket-one/warehouse/db.db/orders/metadata/00001-abc.metadata.json";
    assertThat(
            RewriteTablePathUtil.fileName(
                RewriteTablePathUtil.newPath(uuidMetadata, sourcePrefix, targetPrefix)))
        .isEqualTo("00001-abc.metadata.json");
  }

  @Test
  public void testStagingPathBackwardCompatibility() {
    // Test that the deprecated method still works
    String originalPath = "/some/path/file.parquet";
    String sourcePrefix = "/some/path";
    String stagingDir = "/staging/";

    String result = RewriteTablePathUtil.stagingPath(originalPath, sourcePrefix, stagingDir);

    assertThat(result).isEqualTo("/staging/file.parquet");
  }

  @Test
  public void testStagingPathWithComplexPaths() {
    String sourcePrefix = "/warehouse/db/table";
    String stagingDir = "/tmp/staging/";

    String filePath = "/warehouse/db/table/data/year=2023/month=01/part-00001.parquet";
    String result = RewriteTablePathUtil.stagingPath(filePath, sourcePrefix, stagingDir);

    assertThat(result).isEqualTo("/tmp/staging/data/year=2023/month=01/part-00001.parquet");
  }

  @Test
  public void testStagingPathWithNoMiddlePart() {
    // Test case where file is directly under source prefix (no middle directory structure)
    String sourcePrefix = "/source/table";
    String stagingDir = "/staging/";
    String fileDirectlyUnderPrefix = "/source/table/file.parquet";

    String newMethodResult =
        RewriteTablePathUtil.stagingPath(fileDirectlyUnderPrefix, sourcePrefix, stagingDir);
    assertThat(newMethodResult).isEqualTo("/staging/file.parquet");
  }
}
