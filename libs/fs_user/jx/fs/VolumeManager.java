package jx.fs;

public interface VolumeManager extends jx.zero.Portal {
    Directory getRootDirectory(String volumeSpec);
}
