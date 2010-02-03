/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.crypt;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.obiba.magma.crypt.KeyPairProvider;
import org.obiba.magma.crypt.support.CacheablePasswordCallback;
import org.obiba.magma.crypt.support.CachingCallbackHandler;
import org.obiba.opal.core.service.StudyKeyStoreService;
import org.obiba.opal.core.service.impl.DefaultStudyKeyStoreServiceImpl;

public class OpalKeyStore implements KeyPairProvider {
  //
  // Instance Variables
  //

  private StudyKeyStoreService studyKeyStoreService;

  private CallbackHandler callbackHandler;

  //
  // KeyPairProvider Methods
  //

  public KeyPair getKeyPair(String alias) {
    StudyKeyStore studyKeyStore = studyKeyStoreService.getStudyKeyStore(DefaultStudyKeyStoreServiceImpl.DEFAULT_STUDY_ID);
    if(studyKeyStore == null) {
      throw new IllegalStateException("The key store [" + DefaultStudyKeyStoreServiceImpl.DEFAULT_STUDY_ID + "] does not exist. ");
    }

    KeyStore keyStore = studyKeyStore.getKeyStore();

    KeyPair keyPair = null;

    try {
      CacheablePasswordCallback passwordCallback = new CacheablePasswordCallback(alias, "Password for key " + alias, false);
      Key key = keyStore.getKey(alias, getKeyPassword(passwordCallback));

      if(key == null) {
        throw new KeyPairNotFoundException("KeyPair not found for specified alias (" + alias + ")");
      }

      if(key instanceof PrivateKey) {
        // Get certificate of public key
        Certificate cert = keyStore.getCertificate(alias);

        // Get public key
        PublicKey publicKey = cert.getPublicKey();

        // Return a key pair
        keyPair = new KeyPair(publicKey, (PrivateKey) key);
      } else {
        throw new KeyPairNotFoundException("KeyPair not found for specified alias (" + alias + ")");
      }

      // If the callbackHandler supports it, cache the password.
      if(callbackHandler instanceof CachingCallbackHandler) {
        ((CachingCallbackHandler) callbackHandler).cacheCallbackResult(passwordCallback);
      }
    } catch(KeyPairNotFoundException ex) {
      throw ex;
    } catch(UnrecoverableKeyException ex) {
      throw new KeyProviderSecurityException("Wrong key password");
    } catch(Exception ex) {
      throw new RuntimeException(ex);
    }

    return keyPair;
  }

  public KeyPair getKeyPair(PublicKey publicKey) {
    StudyKeyStore studyKeyStore = studyKeyStoreService.getStudyKeyStore(DefaultStudyKeyStoreServiceImpl.DEFAULT_STUDY_ID);
    if(studyKeyStore == null) {
      throw new IllegalStateException("The key store [" + DefaultStudyKeyStoreServiceImpl.DEFAULT_STUDY_ID + "] does not exist. ");
    }

    KeyStore keyStore = studyKeyStore.getKeyStore();

    Enumeration<String> aliases = null;
    try {
      aliases = keyStore.aliases();
    } catch(KeyStoreException ex) {
      throw new RuntimeException(ex);
    }

    KeyPair keyPair = null;

    while(aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      KeyPair currentKeyPair = getKeyPair(alias);

      if(Arrays.equals(currentKeyPair.getPublic().getEncoded(), publicKey.getEncoded())) {
        keyPair = currentKeyPair;
        break;
      }
    }

    if(keyPair == null) {
      throw new KeyPairNotFoundException("KeyPair not found for specified public key");
    }

    return keyPair;
  }

  //
  // Methods
  //

  public void setCallbackHandler(CallbackHandler callbackHandler) {
    this.callbackHandler = callbackHandler;
  }

  public void setStudyKeyStoreService(StudyKeyStoreService studyKeyStoreService) {
    this.studyKeyStoreService = studyKeyStoreService;
  }

  private char[] getKeyPassword(PasswordCallback passwordCallback) throws UnsupportedCallbackException, IOException {
    callbackHandler.handle(new Callback[] { passwordCallback });
    return passwordCallback.getPassword();
  }
}