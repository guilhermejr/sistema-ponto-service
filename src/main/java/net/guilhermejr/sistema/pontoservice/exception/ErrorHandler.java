package net.guilhermejr.sistema.pontoservice.exception;

import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.pontoservice.exception.dto.ErrorDefaultDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@Log4j2
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ExceptionDefault.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroDefault(ExceptionDefault ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO(ex.getMessage()));

    }

    @ExceptionHandler(ExceptionNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public List<ErrorDefaultDTO> handleErroNotFound(ExceptionNotFound ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO(ex.getMessage()));

    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroParametro(Exception ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO("Parâmetro inválido."));

    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroTamanho(MaxUploadSizeExceededException ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO("O arquivo passa de 1 MB."));

    }

    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroArquivo(Exception ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO("Envie o PDF do espelho de ponto."));

    }

}
