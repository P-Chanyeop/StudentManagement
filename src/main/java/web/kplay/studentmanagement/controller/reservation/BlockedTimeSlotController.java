package web.kplay.studentmanagement.controller.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.reservation.BlockedTimeSlot;
import web.kplay.studentmanagement.dto.reservation.BlockedTimeSlotRequest;
import web.kplay.studentmanagement.dto.reservation.BlockedTimeSlotResponse;
import web.kplay.studentmanagement.repository.BlockedTimeSlotRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/blocked-time-slots")
@RequiredArgsConstructor
public class BlockedTimeSlotController {

    private final BlockedTimeSlotRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<BlockedTimeSlotResponse>> getAll() {
        return ResponseEntity.ok(repository.findByIsActiveTrue().stream()
                .map(BlockedTimeSlotResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlockedTimeSlotResponse> create(@RequestBody BlockedTimeSlotRequest req) {
        BlockedTimeSlot entity = BlockedTimeSlot.builder()
                .blockType(req.getBlockType())
                .blockDate(req.getBlockDate())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .dayOfWeek(req.getDayOfWeek())
                .blockTime(req.getBlockTime())
                .reason(req.getReason())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BlockedTimeSlotResponse.from(repository.save(entity)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        repository.findById(id).ifPresent(b -> {
            b.deactivate();
            repository.save(b);
        });
        return ResponseEntity.noContent().build();
    }
}
