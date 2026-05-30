package com.mealbridge.domain.matching.service;

import com.mealbridge.common.util.HaversineUtil;
import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.domain.matching.client.DonationClient;
import com.mealbridge.domain.matching.client.DonationClient.DonationSnapshot;
import com.mealbridge.domain.matching.client.DonorLocationClient;
import com.mealbridge.domain.matching.client.DonorLocationClient.DonorLocation;
import com.mealbridge.domain.matching.client.OrganizationClient;
import com.mealbridge.domain.matching.client.OrganizationClient.OrganizationSnapshot;
import com.mealbridge.domain.matching.dto.OptimizedRouteResDto;
import com.mealbridge.domain.matching.dto.RouteStopResDto;
import com.mealbridge.domain.matching.entity.RoutePlan;
import com.mealbridge.domain.matching.entity.RoutePlanStatus;
import com.mealbridge.domain.matching.repository.RoutePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteOptimizationService {

	private final OrganizationClient organizationClient;
	private final DonationClient donationClient;
	private final DonorLocationClient donorLocationClient;
	private final RoutePlanRepository routePlanRepository;

	public OptimizedRouteResDto optimize(UUID organizationId, List<UUID> donationIds) {
		return optimize(organizationId, donationIds, true);
	}

	public OptimizedRouteResDto optimize(UUID organizationId, List<UUID> donationIds, boolean persist) {
		if (donationIds == null || donationIds.isEmpty()) {
			return new OptimizedRouteResDto(null, organizationId, List.of(), 0, 0);
		}
		OrganizationSnapshot organization = organizationClient.fetchById(organizationId);
		if (organization.latitude() == null || organization.longitude() == null) {
			throw new IllegalArgumentException("Organization location is required for route optimization");
		}

		List<StopNode> stops = new ArrayList<>();
		for (UUID donationId : donationIds) {
			DonationSnapshot donation = donationClient.fetchById(donationId);
			DonorLocation location = donorLocationClient.fetchLocation(donation.donorInstitutionId());
			if (location.latitude() == null || location.longitude() == null) {
				throw new IllegalArgumentException("Donor location is required for donation: " + donationId);
			}
			stops.add(new StopNode(donation.id(), donation.donorInstitutionId(), location));
		}

		double depotLat = organization.latitude();
		double depotLng = organization.longitude();
		int n = stops.size();
		double[][] matrix = buildDistanceMatrix(depotLat, depotLng, stops);

		List<Integer> tour = nearestNeighbor(matrix, n);
		tour = twoOpt(tour, matrix);

		List<RouteStopResDto> orderedStops = new ArrayList<>();
		long totalDistanceM = 0;
		long totalDurationS = 0;
		int previous = 0;

		for (int i = 0; i < tour.size(); i++) {
			int stopIndex = tour.get(i);
			StopNode stop = stops.get(stopIndex - 1);
			long legDistanceM = Math.round(matrix[previous][stopIndex]);
			long legDurationS = HaversineUtil.durationSeconds(legDistanceM);
			totalDistanceM += legDistanceM;
			totalDurationS += legDurationS;

			orderedStops.add(new RouteStopResDto(
					i + 1,
					stop.donationId(),
					stop.donorInstitutionId(),
					stop.location().name(),
					stop.location().address(),
					stop.location().latitude(),
					stop.location().longitude(),
					legDistanceM,
					legDurationS));
			previous = stopIndex;
		}

		if (!persist) {
			return new OptimizedRouteResDto(
					null,
					organizationId,
					orderedStops,
					totalDistanceM,
					totalDurationS);
		}

		RoutePlan routePlan = RoutePlan.builder()
				.organizationId(organizationId)
				.plannedStopsJson(JsonUtil.toJson(orderedStops))
				.totalDistanceM(totalDistanceM)
				.totalDurationS(totalDurationS)
				.status(RoutePlanStatus.ACTIVE)
				.build();
		routePlanRepository.save(routePlan);

		return new OptimizedRouteResDto(
				routePlan.getId(),
				organizationId,
				orderedStops,
				totalDistanceM,
				totalDurationS);
	}

	private double[][] buildDistanceMatrix(double depotLat, double depotLng, List<StopNode> stops) {
		int size = stops.size() + 1;
		double[][] matrix = new double[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (i == j) {
					matrix[i][j] = 0;
					continue;
				}
				GeoPoint from = pointForIndex(i, depotLat, depotLng, stops);
				GeoPoint to = pointForIndex(j, depotLat, depotLng, stops);
				matrix[i][j] = HaversineUtil.distanceMeters(from.lat(), from.lng(), to.lat(), to.lng());
			}
		}
		return matrix;
	}

	private GeoPoint pointForIndex(int index, double depotLat, double depotLng, List<StopNode> stops) {
		if (index == 0) {
			return new GeoPoint(depotLat, depotLng);
		}
		DonorLocation location = stops.get(index - 1).location();
		return new GeoPoint(location.latitude(), location.longitude());
	}

	private List<Integer> nearestNeighbor(double[][] matrix, int stopCount) {
		List<Integer> tour = new ArrayList<>();
		boolean[] visited = new boolean[stopCount + 1];
		int current = 0;
		visited[0] = true;

		for (int step = 0; step < stopCount; step++) {
			int nearest = -1;
			double nearestDistance = Double.MAX_VALUE;
			for (int candidate = 1; candidate <= stopCount; candidate++) {
				if (visited[candidate]) {
					continue;
				}
				if (matrix[current][candidate] < nearestDistance) {
					nearestDistance = matrix[current][candidate];
					nearest = candidate;
				}
			}
			if (nearest == -1) {
				break;
			}
			tour.add(nearest);
			visited[nearest] = true;
			current = nearest;
		}
		return tour;
	}

	private List<Integer> twoOpt(List<Integer> tour, double[][] matrix) {
		if (tour.size() < 3) {
			return tour;
		}
		List<Integer> best = new ArrayList<>(tour);
		boolean improved = true;
		while (improved) {
			improved = false;
			for (int i = 0; i < best.size() - 1; i++) {
				for (int k = i + 1; k < best.size(); k++) {
					int a = i == 0 ? 0 : best.get(i - 1);
					int b = best.get(i);
					int c = best.get(k);
					int d = k + 1 < best.size() ? best.get(k + 1) : best.get(k);

					double currentDistance = matrix[a][b] + matrix[c][d];
					double swappedDistance = matrix[a][c] + matrix[b][d];
					if (swappedDistance + 1e-6 < currentDistance) {
						reverseSegment(best, i, k);
						improved = true;
					}
				}
			}
		}
		return best;
	}

	private void reverseSegment(List<Integer> tour, int start, int end) {
		while (start < end) {
			Integer temp = tour.get(start);
			tour.set(start, tour.get(end));
			tour.set(end, temp);
			start++;
			end--;
		}
	}

	private record StopNode(UUID donationId, UUID donorInstitutionId, DonorLocation location) {
	}

	private record GeoPoint(double lat, double lng) {
	}
}
