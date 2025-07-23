package com.rhoonart.unearth.song.dto;

/**
 * CSV 곡 데이터 DTO
 */
public class CsvSongDataDto {
    private String artistKo;
    private String artistEn;
    private String albumKo;
    private String albumEn;
    private String titleKo;
    private String titleEn;
    private String youtubeUrl;
    private String melonSongId;
    private String rightHolderName;

    // Builder 패턴
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CsvSongDataDto csvData = new CsvSongDataDto();

        public Builder artistKo(String artistKo) {
            csvData.artistKo = artistKo;
            return this;
        }

        public Builder artistEn(String artistEn) {
            csvData.artistEn = artistEn;
            return this;
        }

        public Builder albumKo(String albumKo) {
            csvData.albumKo = albumKo;
            return this;
        }

        public Builder albumEn(String albumEn) {
            csvData.albumEn = albumEn;
            return this;
        }

        public Builder titleKo(String titleKo) {
            csvData.titleKo = titleKo;
            return this;
        }

        public Builder titleEn(String titleEn) {
            csvData.titleEn = titleEn;
            return this;
        }

        public Builder youtubeUrl(String youtubeUrl) {
            csvData.youtubeUrl = youtubeUrl;
            return this;
        }

        public Builder melonSongId(String melonSongId) {
            csvData.melonSongId = melonSongId;
            return this;
        }

        public Builder rightHolderName(String rightHolderName) {
            csvData.rightHolderName = rightHolderName;
            return this;
        }

        public CsvSongDataDto build() {
            return csvData;
        }
    }

    // Getters
    public String getArtistKo() {
        return artistKo;
    }

    public String getArtistEn() {
        return artistEn;
    }

    public String getAlbumKo() {
        return albumKo;
    }

    public String getAlbumEn() {
        return albumEn;
    }

    public String getTitleKo() {
        return titleKo;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getMelonSongId() {
        return melonSongId;
    }

    public String getRightHolderName() {
        return rightHolderName;
    }

    @Override
    public String toString() {
        return String.format("%s - %s", artistKo, titleKo);
    }
}