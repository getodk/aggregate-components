/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.association;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
@RunWith(org.junit.runners.JUnit4.class)
public class AssociationTest {

  @Test
  public void testGenerateSha1() {
    SecretKey secretKey = Association.generateMacSha1Key();

    assertNotNull(secretKey);
    assertTrue(secretKey instanceof SecretKeySpec);

    SecretKeySpec secretKeySpec = (SecretKeySpec) secretKey;

    assertEquals(Association.HMAC_SHA1_ALGORITHM.toUpperCase(), secretKeySpec.getAlgorithm()
        .toUpperCase());
    assertEquals(20, secretKeySpec.getEncoded().length);
  }

  @Test
  public void testGenerateSha256() {
    if (Association.isHmacSha256Supported()) {
      SecretKey secretKey = Association.generateMacSha256Key();

      assertNotNull(secretKey);
      assertTrue(secretKey instanceof SecretKeySpec);

      SecretKeySpec secretKeySpec = (SecretKeySpec) secretKey;

      assertEquals(Association.HMAC_SHA256_ALGORITHM.toUpperCase(), secretKeySpec.getAlgorithm()
          .toUpperCase());
      assertEquals(32, secretKeySpec.getEncoded().length);
    }
  }

  @Test
  public void testSignSha1() throws AssociationException {
    Association association = Association.generate(Association.TYPE_HMAC_SHA1, "test", 100);

    String macKeyBase64 = new String(Base64.encodeBase64(association.getMacKey().getEncoded()));
    String text = "key1:value1\nkey2:value2\n";

    String signature = association.sign(text);

    assertTrue(association.verifySignature(text, signature));
  }

  @Test
  public void testSignSha256() throws AssociationException {
    Association association = Association.generate(Association.TYPE_HMAC_SHA256, "test", 100);

    String macKeyBase64 = new String(Base64.encodeBase64(association.getMacKey().getEncoded()));
    String text = "key1:value1\nkey2:value2\n";

    String signature = association.sign(text);

    assertTrue(association.verifySignature(text, signature));
  }
}
