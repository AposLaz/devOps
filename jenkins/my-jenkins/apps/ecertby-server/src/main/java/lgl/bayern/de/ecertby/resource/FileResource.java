package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.fuse.cm.service.VersionService;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.model.util.FileType;
import lgl.bayern.de.ecertby.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@Transactional
@RestController
@RequestMapping("files")
@RequiredArgsConstructor
public class FileResource {
    private final VersionService versionService;
    private final FileService fileService;
    @EmptyPredicateCheck
    @GetMapping(path = "findCertificateDoc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE, VIEW_CERTIFICATE}
    )
    public Page<DocumentDTO> findCertificateFile(@PathVariable String id, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return fileService.getDocumentsByType(id, FileType.CERTIFICATE, false);
    }

    @EmptyPredicateCheck
    @GetMapping(path = "findTemplateDoc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
    @OperationAccess(
            operations = { EDIT_TEMPLATE, VIEW_TEMPLATE}
    )
    public Page<DocumentDTO> findTemplateFile(@PathVariable String id) {
        return fileService.getDocumentsByType(id, FileType.TEMPLATE, false);
    }

    @EmptyPredicateCheck
    @GetMapping(path = "findCertificatePreCertificateDocs/{id}/{isPreCertificate}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public Page<DocumentDTO> findCertificatePreCertificateDocs(@PathVariable String id, @PathVariable boolean isPreCertificate, @RequestParam(name = "selectionFromDD", required = false)  @ResourceId String selectionFromDD) {
        return fileService.getDocumentsByType(id,FileType.PRE_CERTIFICATE, isPreCertificate);
    }

    @EmptyPredicateCheck
    @GetMapping(path = "findCertificateAdditionalDocs/{id}/{isPreCertificate}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public Page<DocumentDTO> findCertificateAdditionalFiles(@PathVariable String id, @PathVariable boolean isPreCertificate, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return fileService.getDocumentsByType(id,FileType.ADDITIONAL_DOCUMENT, isPreCertificate);
    }

    @EmptyPredicateCheck
    @GetMapping(path = "findCertificateExternalPreCertificateDocs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public Page<DocumentDTO> findCertificateExternalPreCertificateDocs(@PathVariable String id, @RequestParam(name = "selectionFromDD", required = false)  @ResourceId String selectionFromDD) {
        return fileService.getDocumentsByType(id,FileType.EXTERNAL_PRE_CERTIFICATE, false);
    }

    @EmptyPredicateCheck
    @GetMapping(path = "findCertificateSupplementaryDocs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public Page<DocumentDTO> findCertificateSupplementaryDocs(@PathVariable String id, @RequestParam(name = "selectionFromDD", required = false)  @ResourceId String selectionFromDD) {
        return fileService.getDocumentsByType(id,FileType.SUPPLEMENTARY_CERTIFICATE, false);
    }

    @EmptyPredicateCheck
    @GetMapping(path = "{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  document.")
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public ResponseEntity<byte[]> getDocumentBytes(@PathVariable String id, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return fileService.getDocumentBytes(id);
    }

    @EmptyPredicateCheck
    @GetMapping(path = "{id}/downloadTemplate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  document.")
    @OperationAccess(
            operations = { EDIT_TEMPLATE, VIEW_TEMPLATE }
    )
    public ResponseEntity<byte[]> getTemplateDocumentBytes(@PathVariable String id) {
        return fileService.getDocumentBytes(id);
    }

    @EmptyPredicateCheck
    @PatchMapping(path = "hasVirus", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not scan file for virus.")
    @ResourceAccess(
            operations = { NEW_CERTIFICATE, EDIT_CERTIFICATE, NEW_CATALOG, EDIT_CATALOG }
    )
    public boolean fileHasVirus(@RequestPart("file")MultipartFile file, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return fileService.checkFileForVirus(file);
    }


    @EmptyPredicateCheck
    @PatchMapping(path = "templateHasVirus", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not scan template file for virus.")
    @OperationAccess(
            operations = { NEW_TEMPLATE, EDIT_TEMPLATE }
    )
    public boolean templateFileHasVirus(@RequestPart("file")MultipartFile file) {
        return fileService.checkFileForVirus(file);
    }
}
