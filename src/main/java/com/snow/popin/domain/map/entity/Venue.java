package com.snow.popin.domain.map.entity;

import com.snow.popin.global.common.BaseEntity;
import lombok.Getter;

import javax.persistence.*;

@Entity
@Table(name = "venues")
@Getter
public class Venue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "road_address", length = 500)
    private String roadAddress;

    @Column(name = "jibun_address", length = 500)
    private String jibunAddress;

    @Column(name = "detail_address", length = 500)
    private String detailAddress;

    private Double latitude;
    private Double longitude;

    @Column(length = 100)
    private String region;

    @Column(name = "parking_available")
    private Boolean parkingAvailable = false;

    // 전체 주소를 반환하는 헬퍼 메서드
    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();

        if (roadAddress != null && !roadAddress.trim().isEmpty()) {
            fullAddress.append(roadAddress);
        } else if (jibunAddress != null && !jibunAddress.trim().isEmpty()) {
            fullAddress.append(jibunAddress);
        }

        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            if (fullAddress.length() > 0) {
                fullAddress.append(" ");
            }
            fullAddress.append(detailAddress);
        }

        return fullAddress.toString();
    }

    // 테스트용 메서드
    public static Venue createForTest(String region) {
        Venue venue = new Venue();
        venue.name = "테스트 장소";
        venue.region = region;
        venue.roadAddress = "테스트 도로명 주소";
        venue.jibunAddress = "테스트 지번 주소";
        venue.latitude = 37.5665;
        venue.longitude = 126.9780;
        venue.parkingAvailable = false;
        return venue;
    }

    public static Venue createForTestWithParking(String region, boolean parkingAvailable) {
        Venue venue = createForTest(region);
        venue.parkingAvailable = parkingAvailable;
        return venue;
    }
    //공간등록시 venue 추가 메서드
    public static Venue of(String name,
                           String road, String jibun, String detail,
                           Double lat, Double lng, Boolean parking) {
        Venue v = new Venue();
        v.name = name;
        v.roadAddress = road;
        v.jibunAddress = jibun;
        v.detailAddress = detail;
        v.latitude = lat;
        v.longitude = lng;
        v.parkingAvailable = parking != null ? parking : false;
        return v;
    }
    public void update(String name, String roadAddress, String jibunAddress, String detailAddress,
                       Double latitude, Double longitude, Boolean parkingAvailable) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.detailAddress = detailAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.parkingAvailable = parkingAvailable != null ? parkingAvailable : false;
    }

    // 테스트용 메서드
    public static Venue createForTestWithLocation(String region, double latitude, double longitude) {
        Venue venue = new Venue();
        venue.name = "테스트 장소";
        venue.region = region;
        venue.roadAddress = "테스트 도로명 주소";
        venue.jibunAddress = "테스트 지번 주소";
        venue.latitude = latitude;
        venue.longitude = longitude;
        venue.parkingAvailable = false;
        return venue;
    }
    //region  메서드
    public void setRegionFromAddress() {
        if (this.roadAddress != null) {
            String[] parts = this.roadAddress.trim().split(" ");
            if (parts.length > 0) {
                this.region = parts[0];  // ex) "서울"
            }
        } else if (this.jibunAddress != null) {
            String[] parts = this.jibunAddress.trim().split(" ");
            if (parts.length > 0) {
                this.region = parts[0];
            }
        }
    }

}
