// Steven Kester Yuwono
// A0080415N

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;

// Amy knows Bryan's public key
// Amy sends Bryan session (AES) key
// Amy receives messages from Bryan, decrypts and saves them to file

class Amy {  // Amy is a TCP client
    
    String bryanIP;  // ip address of Bryan
    int bryanPort;   // port Bryan listens to
    Socket connectionSkt;  // socket used to talk to Bryan
    private ObjectOutputStream toBryan;   // to send session key to Bryan
    private ObjectInputStream fromBryan;  // to read encrypted messages from Bryan
    private Crypto crypto;        // object for encryption and decryption
    // file to store received and decrypted messages
    public static final String MESSAGE_FILE = "msgs.txt";
    
    public static void main(String[] args) {
        
        // Check if the number of command line argument is 2
        if (args.length != 2) {
            System.err.println("Usage: java Amy BryanIP BryanPort");
            System.exit(1);
        }
        
        new Amy(args[0], args[1]);
    }
    
    // Constructor
    public Amy(String ipStr, String portStr) {
        bryanIP = ipStr;
        bryanPort = Integer.parseInt(portStr);

        this.crypto = new Crypto();

        try {
            this.connectionSkt = new Socket(bryanIP, bryanPort);
        } catch (IOException ioe) {
            System.out.println("Error in connecting to Bryan");
            System.exit(1);
        }
        

        try {
            this.toBryan = new ObjectOutputStream(this.connectionSkt.getOutputStream());
            this.fromBryan = new ObjectInputStream(this.connectionSkt.getInputStream());
        } catch (IOException ioe) {
            System.out.println("Error: cannot get input/output streams");
            System.exit(1);
        }


        // Get Public key from Bryan
        getPublicKey();
        
        // Send session key to Bryan
        sendSessionKey();
        
        // Receive encrypted messages from Bryan,
        // decrypt and save them to file
        receiveMessages();
    }
    
    // Send session key to Bryan
    public void sendSessionKey() {
        try {
            toBryan.writeObject(crypto.getSessionKey());
        } catch (IOException ioe) {
            System.out.println("Error: cannot send session key to Bryan!");
            System.exit(1);
        }
        
    }
    
    // Receive messages one by one from Bryan, decrypt and write to file
    public void receiveMessages() { 
        try {
            PrintWriter pw = new PrintWriter(new File("msgs.txt"));
            for (int i=0;i<10;i++) {
                SealedObject sealedMessage = (SealedObject)this.fromBryan.readObject();
                String message = crypto.decryptMsg(sealedMessage);
                pw.println(message);
            }
            pw.close();

            System.out.println("All messages are received from Bryan");
        } catch (ClassNotFoundException ioe) {
            System.out.println("Error: message from Bryan cannot typecast to class SealedObject");
            System.exit(1); 
        } catch (IOException ioe) {
            System.out.println("Error receiving messages from Bryan");
            System.exit(1);
        }
    }

    public void getPublicKey() {
        try {
            PublicKey bryanPublicKey = (PublicKey)this.fromBryan.readObject();
            byte[] encryptedDigest = (byte[])this.fromBryan.readObject();

            crypto.checkAndSetPublicKey(bryanPublicKey,encryptedDigest);

            System.out.println("Public key are successfully received from Bryan");
        } catch (ClassNotFoundException ioe) {
            System.out.println("Error: publicKey from Bryan cannot typecast");
            System.exit(1); 
        } catch (IOException ioe) {
            System.out.println("Error receiving publicKey from Bryan");
            System.exit(1);
        }
    }
    
    /*****************/
    /** inner class **/
    /*****************/
    class Crypto {
        
        // Bryan's public key, to be read from file
        private PublicKey pubKey;
        // Amy generates a new session key for each communication session
        private SecretKey sessionKey;

        private PublicKey berisignPubKey;
        // File that contains Bryan' public key
        public static final String PUBLIC_KEY_FILE = "berisign.pub";
        
        // Constructor
        public Crypto() {
            // Read Berisign's public key from file
            readBerisignPublicKey();
            // Generate session key dynami  cally
            initSessionKey();
        }

        public byte[] serialize(Object obj) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
        }
        
        // Read Bryan's public key from file
        public void checkAndSetPublicKey(PublicKey currentKey, byte[] encryptedDigest) {
            byte[] currentDigest = null;
            try {
                // get bytes for string bryan
                String name = "bryan";
                byte[] byteName = name.getBytes("US-ASCII");
                byte[] byteCurrentKey = currentKey.getEncoded();

                MessageDigest currentMd5 = MessageDigest.getInstance("MD5");
                currentMd5.update(byteName);
                currentMd5.update(byteCurrentKey);
                currentDigest = currentMd5.digest();  
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            byte[] decryptedDigest = null;
            try {
                // Amy and Bryan use the same AES key/transformation
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, berisignPubKey);
                decryptedDigest = cipher.doFinal(encryptedDigest);
            } catch (GeneralSecurityException gse) {
                System.out.println("Error:MD5 signature does not match");
                // gse.printStackTrace();
                System.exit(1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Check whether the digest is valid (The same)
            // if yes then the public key is indeed from Bryan
            if (MessageDigest.isEqual(currentDigest, decryptedDigest)) {
                pubKey = currentKey;
            }
            else {
                System.out.println("Error:MD5 signature does not match");
                System.exit(1);
            }
        }
        
        // Read Berisign public key     
        public void readBerisignPublicKey() {
            // key is stored as an object and need to be read using ObjectInputStream.
            // See how Bob read his private key as an example.
            try {
                ObjectInputStream ois = 
                    new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
                this.berisignPubKey = (PublicKey)ois.readObject();
                ois.close();
            } catch (IOException oie) {
                System.out.println("Error reading public key from file");
                System.exit(1);
            } catch (ClassNotFoundException cnfe) {
                System.out.println("Error: cannot typecast to class PublicKey");
                System.exit(1);
            }
            
            System.out.println("Public key read from file " + PUBLIC_KEY_FILE);
        }

        // Generate a session key
        public void initSessionKey() {
            // suggested AES key length is 128 bits
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128); // for example
                sessionKey = keyGen.generateKey();
            } catch (GeneralSecurityException gse) {
                
            }
        }
        
        // Seal session key with RSA public key in a SealedObject and return
        public SealedObject getSessionKey() {
            // RSA imposes size restriction on the object being encrypted (117 bytes).
            // Instead of sealing a Key object which is way over the size restriction,
            // we shall encrypt AES key in its byte format (using getEncoded() method).  
            SealedObject sessionKeyObj = null;

            try {
                // Amy must use the same RSA key/transformation as Bryan specified
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, pubKey);

                byte[] byteSessionKey = sessionKey.getEncoded();
                sessionKeyObj = new SealedObject(byteSessionKey, cipher);
            } catch (GeneralSecurityException gse) {
                System.out.println("Error: wrong cipher to encrypt session key");
                System.exit(1);
            } catch (IOException ioe) {
                System.out.println("Error creating session key SealedObject");
                System.exit(1);
            }
            return sessionKeyObj;   
        }
        
        // Decrypt and extract a message from SealedObject
        public String decryptMsg(SealedObject encryptedMsgObj) {
            
            String plainText = null;
            
            try {
                // Amy and Bryan use the same AES key/transformation
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, sessionKey);
                plainText = (String) encryptedMsgObj.getObject(cipher);

            } catch (GeneralSecurityException gse) {
                System.out.println("Error: wrong cipher to decrypt message");
                System.exit(1);
            } catch (IOException ioe) {
                System.out.println("Error creating SealedObject");
                System.exit(1);
            } catch (ClassNotFoundException ioe) {
                System.out.println("Error: cannot typecast to byte array");
                System.exit(1); 
            } 
            
            return plainText;
        }
    }
}
