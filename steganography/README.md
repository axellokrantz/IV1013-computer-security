# IV1013 Steganography
## Program usage:
Compilation: `$ javac Steganography.java`  
Help command: `$ java Steganography -help`

## Encoding:
1. Prepare an image in file format .png
2. Create a secret message text file.
3. Choose a value of n within the range of 1-8.
4. Choose a name for the output in file format .png
5. Encode: `$ java Steganographer <input-image> <input-secret-message> <n> <output-image>`
6. The program will output an encrypted image with the secret message.

## Decoding:
1. Prepare an encrypted image in file format .png
2. Choose the same n that was used for encrypting the image.
3. Choose a name for secret message file
4. Decode: `$ java Steganographer <input-image> <n> <output-message>`
5. The program will extract the secret message from the image.
