/**
 * Double-hashing according to Schnell, Bachteler and Reiher,
 * p. 5.
 *
 * @param val String The string to hash.
 * @param i Integer Index of the hash function.
 * @param l Integer	Lenght of the Bloom filter.
 * 
 * @return The index of the bit to set in the Bloom filter.  
 */
function hash(val, i, l)
{
	var hash1 = Crypto.SHA1(val, {asBytes: true});
	var hash2 = Crypto.MD5(val, {asBytes: true});
	
	// reduce hashes to significant bytes
	var nSignBytes = Math.ceil(Math.log(l) / Math.log(256));
	var hash1Val = 0;
	var hash2Val = 0;
	// convert significant (lower) bytes of hash to integer
	for (var byteInd = 0; byteInd < nSignBytes; byteInd++)
	{
    hash1Val += Math.pow(256, byteInd) * hash1[hash1.length - 1 - byteInd];
    hash2Val += Math.pow(256, byteInd) * hash2[hash2.length - 1 - byteInd];
  }
	
	return (hash1Val + i * hash2Val) % l;
}

/** Bloom filter of a string.
 *
 *  @param x String The string for which to calculate the Bloom filter.
 *  @param k Integer The number of hash functions.
 *  @param l Integer Size of the Bloom filter.
 *  
 * @return Array The Bloom filter, as array of positions which are set.
 */ 
function bloomFilter(x, k, l)
{ 
  var bloom = new Array(k);
  // Calulate all of the k hash functions and set returned position 
  for (var hashInd = 0; hashInd < k; hashInd++)
  {
    bloom[hashInd] = hash(x, hashInd, l);
  }
  return bloom;
}

/** Calculate Bloom filter for n-grams.
 *  A Bloom filter is calculated for every n-gram in x, x being padded by n-1
 *  spaces to the left and to the right. The resulting filters are combined by
 *  logical or.
 *  
 *  @param x String The string for which to calculate the Bloom filter.
 *  @param n Integer Size of the n-grams.
 *  @param k Integer Number of hash functions.
 *  @param l Integer Length of the Bloom filter.
 *  
 *  @return Array  The Bloom filter, set positions are encoded as true.     
 *
 */ 
function nGramBloomFilter(x, n, k, l)
{
  var bloom = Array(l);
  // Add padding to string
  var tempStr = x;
  for (var i=0; i<n-1; i++)
  {
    tempStr = " " + tempStr + " ";
  }
  
  for (var i=0; i <= tempStr.length - n; i++)
  {
    var nGram = tempStr.substr(i, n);
    thisBloom = bloomFilter(nGram, k, l);
    /* Set the corresponding positions for every nGram in the String */ 
    for (var arrayInd=0; arrayInd < thisBloom.length; arrayInd++)
    {
      bloom[thisBloom[arrayInd]] = 1;
    }
  }
  
  return bloom;
  
} 
