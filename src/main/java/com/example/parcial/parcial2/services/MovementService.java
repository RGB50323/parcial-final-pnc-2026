package com.example.parcial.parcial2.services;

import com.example.parcial.parcial2.domain.dtos.MovementRequestDto;
import com.example.parcial.parcial2.domain.entities.Book;
import com.example.parcial.parcial2.domain.entities.Lector;
import com.example.parcial.parcial2.domain.entities.Movement;
import com.example.parcial.parcial2.domain.entities.MovementType;
import com.example.parcial.parcial2.repositories.BookRepository;
import com.example.parcial.parcial2.repositories.LectorRepository;
import com.example.parcial.parcial2.repositories.MovementRepository;
import jakarta.transaction.Transactional;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final LectorRepository lectorRepository;
    private final BookRepository bookRepository;

    public MovementService(MovementRepository movementRepository,
                           LectorRepository lectorRepository,
                           BookRepository bookRepository) {
        this.movementRepository = movementRepository;
        this.lectorRepository = lectorRepository;
        this.bookRepository = bookRepository;
    }

    public Book findBook(String isbn) throws BadRequestException {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BadRequestException("Libro no encontrado"));
    }

    public Lector findLector(String email) throws BadRequestException {
        return lectorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Lector no encontrado"));
    }

    @Transactional
    public Movement borrowBook(MovementRequestDto dto) throws BadRequestException {
        Book book = findBook(dto.getIsbn());
        Lector lector = findLector(dto.getEmail());

        Optional<Movement> last = movementRepository.findTopByBookAndLectorOrderByTimestampDesc(book, lector);
        if (last.isPresent() && last.get().getType() == MovementType.BORROWING) {
            throw new BadRequestException("El lector ya tiene este libro prestado");
        }

        if (book.getAvailableCount() <= 0) {
            throw new BadRequestException("No hay copias disponibles de este libro");
        }

        book.setAvailableCount(book.getAvailableCount() - 1);
        book.setAvailable(book.getAvailableCount() > 0);
        bookRepository.save(book);

        Movement movement = new Movement();
        movement.setBook(book);
        movement.setLector(lector);
        movement.setTimestamp(Instant.now());
        movement.setType(MovementType.BORROWING);
        return movementRepository.save(movement);
    }

    @Transactional
    public Movement returnBook(MovementRequestDto dto) throws BadRequestException {
        Book book = findBook(dto.getIsbn());
        Lector lector = findLector(dto.getEmail());

        Movement last = movementRepository.findTopByBookAndLectorOrderByTimestampDesc(book, lector)
                .orElseThrow(() -> new BadRequestException(
                        "Este lector nunca ha solicitado en préstamo este libro"));

        if (last.getType() != MovementType.BORROWING) {
            throw new BadRequestException("Este libro ya fue devuelto");
        }

        book.setAvailableCount(book.getAvailableCount() + 1);
        book.setAvailable(true);
        bookRepository.save(book);

        Movement movement = new Movement();
        movement.setBook(book);
        movement.setLector(lector);
        movement.setTimestamp(Instant.now());
        movement.setType(MovementType.RETURN);
        return movementRepository.save(movement);
    }

    private Movement createMovement(MovementRequestDto dto, MovementType type) {
        Lector lector = lectorRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Lector not found"));

        Book book = bookRepository.findByIsbn(dto.getIsbn())
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if (type == MovementType.BORROWING) {
            if (!book.isAvailable()) {
                throw new RuntimeException("Book is not available");
            }
            book.setAvailableCount(book.getAvailableCount() - 1);
            if (book.getAvailableCount() == 0) {
                book.setAvailable(false);
            }
        } else {
            book.setAvailableCount(book.getAvailableCount() + 1);
        }

        bookRepository.save(book);

        Movement movement = new Movement();
        movement.setLector(lector);
        movement.setBook(book);
        movement.setTimestamp(Instant.now());
        movement.setType(type);

        return movementRepository.save(movement);
    }
}
