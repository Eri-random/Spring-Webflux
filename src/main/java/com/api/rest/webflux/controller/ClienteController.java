package com.api.rest.webflux.controller;

import com.api.rest.webflux.documentos.Cliente;
import com.api.rest.webflux.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cliente")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Value("${config.uploads.path}")
    private String path;

    @PostMapping("/v2")
    public Mono<ResponseEntity<Cliente>> registrarClienteConFoto(Cliente cliente, @RequestPart FilePart file){

        cliente.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                .replace(" ","")
                .replace(":","")
                .replace("//",""));

        return file.transferTo(new File(path + cliente.getFoto())).then(clienteService.save(cliente))
                .map(c -> ResponseEntity.created(URI.create("/api/clientes/".concat(c.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(c));

    }

    @PostMapping("/upload/{id}")
    public Mono<ResponseEntity<Cliente>> subirFoto(@PathVariable String id, @RequestPart FilePart file){
        return clienteService.findById(id).flatMap( c -> {
            c.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                    .replace(" ","")
                    .replace(":","")
                    .replace("//",""));;

            return file.transferTo(new File(path + c.getFoto())).then(clienteService.save(c));
        }).map(c -> ResponseEntity.ok(c))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<Cliente>>> listarClientes(){
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(clienteService.findAll())
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Cliente>> obtenerCliente(@PathVariable String id){
        return clienteService.findById(id).map(c -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(c))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Cliente>> guardarCliente(@Valid @RequestBody Cliente cliente){
        return clienteService.save(cliente)
                .map(clienteGuardado -> new ResponseEntity<>(clienteGuardado,HttpStatus.ACCEPTED))
                .defaultIfEmpty(new ResponseEntity<>(cliente,HttpStatus.NOT_ACCEPTABLE));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Cliente>> actualizarCliente(@RequestBody Cliente cliente,@PathVariable String id){

        return clienteService.findById(id).flatMap(c ->{
            c.setNombre(cliente.getNombre());
            c.setApellido(cliente.getApellido());
            c.setEdad(cliente.getEdad());
            c.setSueldo(cliente.getSueldo());
            return clienteService.save(c);
        }).map( c -> ResponseEntity.created(URI.create("/api/clientes/".concat(c.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(c))
                .defaultIfEmpty(ResponseEntity.notFound().build());

    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id){
        return clienteService.findById(id).flatMap(c -> clienteService.delete(c)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))).defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


}
