import 'package:flutter/material.dart';
import 'package:mapboxnav/mapboxnav.dart';
import 'dart:async';

import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mapbox Navigation Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const NavigationScreen(),
    );
  }
}

class NavigationScreen extends StatefulWidget {
  const NavigationScreen({Key? key}) : super(key: key);

  @override
  State<NavigationScreen> createState() => _NavigationScreenState();
}

class _NavigationScreenState extends State<NavigationScreen> {
  MapboxNavigationController? _controller; // Agora é anulável, pois é inicializado após a criação da view
  StreamSubscription? _eventSubscription; // Para gerenciar a subscrição de eventos

  String _navigationStatus = 'Aguardando inicialização da navegação...';
  String _currentInstruction = 'Nenhuma instrução.';
  String _eta = 'Calculando...';
  String _distanceRemaining = 'Calculando...';

  final List<double> _origin = [-8.814655, 13.230176]; // Banco Nacional de Angola (BNA), Rua 1º de Maio, Luanda
  final List<double> _destination = [-8.811000, 13.234000]; // Banco Angolano de Investimentos (BAI), Edifício Escom, Kinaxixi, Luanda
  final List<double> _newDestination = [-8.823000, 13.242000]; // Banco Millennium Atlântico, Cidade Financeira, Talatona, Luanda
  bool _permissionsGranted = false; // Para rastrear o status das permissões
  @override
  void initState() {
    super.initState();
    _checkAndRequestLocationPermissions();

  }
  Future<void> _checkAndRequestLocationPermissions() async {
    var status = await Permission.locationWhenInUse.status;
    print('Permissão de localização status inicial: $status');

    if (status.isDenied || status.isPermanentlyDenied) {
      status = await Permission.locationWhenInUse.request();
      print('Permissão de localização status após solicitação: $status');
    }

    if (status.isGranted) {
      setState(() {
        _permissionsGranted = true;
      });
      print('Permissão de localização concedida!');
    } else {
      setState(() {
        _permissionsGranted = false;
      });
      print('Permissão de localização negada ou permanentemente negada.');
    }
  }


  @override
  void dispose() {
    _eventSubscription?.cancel();
    _controller?.dispose();
    super.dispose();
  }

  void _onMapboxViewCreated(MapboxNavigationController controller) {
    setState(() {
      _controller = controller;
      _navigationStatus = 'MapboxNavigationView inicializado.';
    });
    _eventSubscription = _controller!.events.listen(_handleNavigationEvent);
  }

  void _handleNavigationEvent(MapboxNavigationEvent event) {
    setState(() {
      _navigationStatus = 'Último evento: ${event['type']}';
      switch (event['type']) {
        case 'pluginInitialized':
          _navigationStatus = 'Plugin inicializado! View ID: ${event['viewId']}';
          break;
        case 'locationPermissionsGranted':
          _navigationStatus = 'Permissões de localização concedidas.';
          break;
        case 'locationUpdate':
          _navigationStatus = 'Localização: Lat ${event['latitude']?.toStringAsFixed(4)}, Lng ${event['longitude']?.toStringAsFixed(4)}';
          break;
        case 'routeCreated':
          _navigationStatus = 'Rota criada! Rotas encontradas: ${event['routeCount']}';
          _currentInstruction = 'Rota pronta para iniciar.';
          break;
        case 'navigationStarted':
          _navigationStatus = 'Navegação iniciada!';
          break;
        case 'routeProgressUpdate':
          _distanceRemaining = '${(event['distanceRemaining'] as double?)?.toStringAsFixed(0) ?? 'N/A'} m';
          _eta = '${(event['durationRemaining'] as double?)?.toStringAsFixed(0) ?? 'N/A'} seg';
          _currentInstruction = event['nextInstructionText'] ?? 'Nenhuma instrução.';
          _navigationStatus = 'Progresso: Distância: $_distanceRemaining, ETA: $_eta';
          break;
        case 'routesChanged':
          _navigationStatus = 'Rotas atualizadas! Contagem: ${event['routeCount']}';
          break;
        case 'destinationChanged':
          _navigationStatus = 'Destino alterado para Lat: ${event['newDestinationLat']?.toStringAsFixed(4)}, Lng: ${event['newDestinationLng']?.toStringAsFixed(4)}';
          break;
        case 'navigationCancelled':
          _navigationStatus = 'Navegação Cancelada.';
          _currentInstruction = 'Nenhuma instrução.';
          _eta = 'N/A';
          _distanceRemaining = 'N/A';
          break;
        case 'navigationFinished':
          _navigationStatus = 'Navegação Finalizada!';
          _currentInstruction = 'Chegou ao destino.';
          _eta = '0 seg';
          _distanceRemaining = '0 m';
          break;
        case 'tripSessionStateChanged':
          _navigationStatus = 'Estado da sessão: ${event['state']}';
          break;
        case 'error':
          _navigationStatus = 'Erro: ${event['message']}';
          break;
        default:
          _navigationStatus = 'Evento genérico: ${event['type']}';
          break;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mapbox Navigation Plugin'),
      ),
      body: Container(
        width: MediaQuery.of(context).size.width,
        height:MediaQuery.of(context).size.height ,
        child: Column(
          children: [
            Expanded(
              flex: 3,
              child: Container(
                color: Colors.grey[200],
                child: MapboxNavigationView(
                  onMapboxViewCreated: _onMapboxViewCreated, // Passe o callback
                ),
              ),
            ),
            Expanded(
              flex: 1,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Container(
                    height: 500,
                    width: MediaQuery.of(context).size.width,
                    child:SingleChildScrollView(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(_navigationStatus, style: const TextStyle(fontSize: 16)),
                          const SizedBox(height: 8),
                          Text('Instrução: $_currentInstruction', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                          Text('ETA: $_eta, Distância: $_distanceRemaining', style: const TextStyle(fontSize: 16)),
                          Wrap(
                            spacing: 8.0,
                            runSpacing: 4.0,
                            children: [
                              // Os botões só devem estar habilitados se o _controller não for nulo
                              ElevatedButton(
                                onPressed: _controller == null
                                    ? null
                                    : () {
                                  _controller!.createRoute(
                                    origin: _origin,
                                    destination: _destination,
                                  );
                                },
                                child: const Text('Criar Rota'),
                              ),
                              ElevatedButton(
                                onPressed: _controller == null
                                    ? null
                                    : () {
                                  _controller!.startNavigation(
                                    origin: _origin,
                                    destination: _destination,
                                  );
                                },
                                child: const Text('Iniciar Corrida'),
                              ),
                              ElevatedButton(
                                onPressed: _controller == null
                                    ? null
                                    : () {
                                  _controller!.changeDestination(
                                    origin: _origin,
                                    newDestination: _newDestination,
                                  );
                                },
                                child: const Text('Mudar Destino'),
                              ),
                              ElevatedButton(
                                onPressed: _controller == null
                                    ? null
                                    : () {
                                  _controller!.cancelNavigation();
                                },
                                child: const Text('Cancelar Corrida'),
                              ),
                              ElevatedButton(
                                onPressed: _controller == null
                                    ? null
                                    : () {
                                  _controller!.finishNavigation();
                                },
                                child: const Text('Finalizar Corrida'),
                              ),
                            ],
                          ),
                        ],
                      ),
                    )
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}