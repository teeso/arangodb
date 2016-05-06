// Copyright (c) 2011-present, Facebook, Inc.  All rights reserved.
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree. An additional grant
// of patent rights can be found in the PATENTS file in the same directory.

package org.rocksdb;

/**
 * Similar to {@link org.rocksdb.WriteBatch} but with a binary searchable
 * index built for all the keys inserted.
 *
 * Calling put, merge, remove or putLogData calls the same function
 * as with {@link org.rocksdb.WriteBatch} whilst also building an index.
 *
 * A user can call {@link org.rocksdb.WriteBatchWithIndex#newIterator()} to
 * create an iterator over the write batch or
 * {@link org.rocksdb.WriteBatchWithIndex#newIteratorWithBase(org.rocksdb.RocksIterator)}
 * to get an iterator for the database with Read-Your-Own-Writes like capability
 */
public class WriteBatchWithIndex extends AbstractWriteBatch {
  /**
   * Creates a WriteBatchWithIndex where no bytes
   * are reserved up-front, bytewise comparison is
   * used for fallback key comparisons,
   * and duplicate keys operations are retained
   */
  public WriteBatchWithIndex() {
    super(newWriteBatchWithIndex());
  }


  /**
   * Creates a WriteBatchWithIndex where no bytes
   * are reserved up-front, bytewise comparison is
   * used for fallback key comparisons, and duplicate key
   * assignment is determined by the constructor argument
   *
   * @param overwriteKey if true, overwrite the key in the index when
   *   inserting a duplicate key, in this way an iterator will never
   *   show two entries with the same key.
   */
  public WriteBatchWithIndex(final boolean overwriteKey) {
    super(newWriteBatchWithIndex(overwriteKey));
  }

  /**
   * Creates a WriteBatchWithIndex
   *
   * @param fallbackIndexComparator We fallback to this comparator
   *  to compare keys within a column family if we cannot determine
   *  the column family and so look up it's comparator.
   *
   * @param reservedBytes reserved bytes in underlying WriteBatch
   *
   * @param overwriteKey if true, overwrite the key in the index when
   *   inserting a duplicate key, in this way an iterator will never
   *   show two entries with the same key.
   */
  public WriteBatchWithIndex(
      final AbstractComparator<? extends AbstractSlice<?>>
          fallbackIndexComparator, final int reservedBytes,
      final boolean overwriteKey) {
    super(newWriteBatchWithIndex(fallbackIndexComparator.getNativeHandle(),
        reservedBytes, overwriteKey));
  }

  /**
   * Create an iterator of a column family. User can call
   * {@link org.rocksdb.RocksIteratorInterface#seek(byte[])} to
   * search to the next entry of or after a key. Keys will be iterated in the
   * order given by index_comparator. For multiple updates on the same key,
   * each update will be returned as a separate entry, in the order of update
   * time.
   *
   * @param columnFamilyHandle The column family to iterate over
   * @return An iterator for the Write Batch contents, restricted to the column
   * family
   */
  public WBWIRocksIterator newIterator(
      final ColumnFamilyHandle columnFamilyHandle) {
    return new WBWIRocksIterator(this, iterator1(nativeHandle_,
            columnFamilyHandle.nativeHandle_));
  }

  /**
   * Create an iterator of the default column family. User can call
   * {@link org.rocksdb.RocksIteratorInterface#seek(byte[])} to
   * search to the next entry of or after a key. Keys will be iterated in the
   * order given by index_comparator. For multiple updates on the same key,
   * each update will be returned as a separate entry, in the order of update
   * time.
   *
   * @return An iterator for the Write Batch contents
   */
  public WBWIRocksIterator newIterator() {
    return new WBWIRocksIterator(this, iterator0(nativeHandle_));
  }

  /**
   * Provides Read-Your-Own-Writes like functionality by
   * creating a new Iterator that will use {@link org.rocksdb.WBWIRocksIterator}
   * as a delta and baseIterator as a base
   *
   * @param columnFamilyHandle The column family to iterate over
   * @param baseIterator The base iterator,
   *   e.g. {@link org.rocksdb.RocksDB#newIterator()}
   * @return An iterator which shows a view comprised of both the database
   * point-in-time from baseIterator and modifications made in this write batch.
   */
  public RocksIterator newIteratorWithBase(
      final ColumnFamilyHandle columnFamilyHandle,
      final RocksIterator baseIterator) {
    RocksIterator iterator = new RocksIterator(
        baseIterator.parent_,
        iteratorWithBase(nativeHandle_,
                columnFamilyHandle.nativeHandle_,
                baseIterator.nativeHandle_));
    //when the iterator is deleted it will also delete the baseIterator
    baseIterator.disOwnNativeHandle();
    return iterator;
  }

  /**
   * Provides Read-Your-Own-Writes like functionality by
   * creating a new Iterator that will use {@link org.rocksdb.WBWIRocksIterator}
   * as a delta and baseIterator as a base. Operates on the default column
   * family.
   *
   * @param baseIterator The base iterator,
   *   e.g. {@link org.rocksdb.RocksDB#newIterator()}
   * @return An iterator which shows a view comprised of both the database
   * point-in-timefrom baseIterator and modifications made in this write batch.
   */
  public RocksIterator newIteratorWithBase(final RocksIterator baseIterator) {
    return newIteratorWithBase(baseIterator.parent_.getDefaultColumnFamily(),
        baseIterator);
  }

  @Override protected final native void disposeInternal(final long handle);
  @Override final native int count0(final long handle);
  @Override final native void put(final long handle, final byte[] key,
      final int keyLen, final byte[] value, final int valueLen);
  @Override final native void put(final long handle, final byte[] key,
      final int keyLen, final byte[] value, final int valueLen,
      final long cfHandle);
  @Override final native void merge(final long handle, final byte[] key,
      final int keyLen, final byte[] value, final int valueLen);
  @Override final native void merge(final long handle, final byte[] key,
      final int keyLen, final byte[] value, final int valueLen,
      final long cfHandle);
  @Override final native void remove(final long handle, final byte[] key,
      final int keyLen);
  @Override final native void remove(final long handle, final byte[] key,
      final int keyLen, final long cfHandle);
  @Override final native void putLogData(final long handle, final byte[] blob,
      final int blobLen);
  @Override final native void clear0(final long handle);

  private native static long newWriteBatchWithIndex();
  private native static long newWriteBatchWithIndex(final boolean overwriteKey);
  private native static long newWriteBatchWithIndex(
      final long fallbackIndexComparatorHandle, final int reservedBytes,
      final boolean overwriteKey);
  private native long iterator0(final long handle);
  private native long iterator1(final long handle, final long cfHandle);
  private native long iteratorWithBase(final long handle,
      final long baseIteratorHandle, final long cfHandle);
}
