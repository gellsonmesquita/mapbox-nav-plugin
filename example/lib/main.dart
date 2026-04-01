import 'package:flutter/material.dart';
import 'package:mapboxnav/mapboxnav.dart';
import 'dart:async';

import 'package:permission_handler/permission_handler.dart';
import 'package:mapboxnav/src/data/performance_policy.dart';

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
  MapboxNavigationController? _controller;
  StreamSubscription? _eventSubscription; 

  String _navigationStatus = 'Aguardando inicialização da navegação...';
  String _currentInstruction = 'Nenhuma instrução.';
  String _eta = 'Calculando...';
  String _distanceRemaining = 'Calculando...';
  String _dataSaverMode = 'OFF';
  String _infoMessage = '';

  PerformancePolicy? _customPolicy;
  String _downloadStatus = '';
  bool _isDownloading = false;

  final List<double> _origin = [-8.814655, 13.230176]; // Banco Nacional de Angola (BNA), Rua 1º de Maio, Luanda
  final List<double> _destination = [-8.811000, 13.234000]; // Banco Angolano de Investimentos (BAI), Edifício Escom, Kinaxixi, Luanda
  final List<double> _newDestination = [-8.823000, 13.242000]; // Banco Millennium Atlântico, Cidade Financeira, Talatona, Luanda

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
      print('Permissão de localização concedida!');
    } else {
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
    _controller!.setPerformancePolicy(PerformancePolicy.defaults());
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
        case 'offlineDownloadBlocked':
          _infoMessage = 'Download offline bloqueado: só é permitido em Wi-Fi no modo economia de dados.';
          _isDownloading = false;
          _downloadStatus = 'Bloqueado (apenas Wi-Fi)';
          break;
        case 'offlineDownloadComplete':
          if (event['status'] == 'already_exists') {
            _infoMessage = 'A região já está baixada. Nenhum dado extra foi consumido.';
            _downloadStatus = 'Região já baixada';
          } else {
            _infoMessage = 'Download offline concluído com sucesso!';
            _downloadStatus = 'Download concluído';
          }
          _isDownloading = false;
          break;
        case 'tripProgressUpdate':
        case 'maneuverUpdate':
          if (_dataSaverMode == 'AGRESSIVE') {
            _infoMessage = 'Eventos de navegação reduzidos para economizar dados.';
          } else {
            _infoMessage = '';
          }
          break;
        default:
          _infoMessage = '';
          _isDownloading = false;
          break;
      }
    });
  }

  // Exemplo de PerformancePolicy customizada
  PerformancePolicy _buildCustomPolicy() {
    return PerformancePolicy(
      routeRequestCooldownMs: 8000,
      locationEventMinIntervalMs: 2000,
      tripProgressEventMinIntervalMs: 2000,
      offlineProgressEventMinIntervalMs: 2000,
      skipDuplicateRouteRequests: true,
      locationGranularity: LocationGranularity.balanced,
      reduceFlutterEvents: true,
      enableRouteCache: true,
      enableTelemetry: false,
      dataSaverMode: DataSaverMode.balanced,
      allowOfflineDownloadOnCellular: false,
      forceOfflineRedownload: false,
    );
  }

  void _setDataSaverMode(String mode) async {
    if (_controller != null) {
      PerformancePolicy policy;
      switch (mode) {
        case 'OFF':
          policy = PerformancePolicy.off();
          break;
        case 'BALANCED':
          policy = PerformancePolicy.balanced();
          break;
        case 'AGGRESSIVE':
          policy = PerformancePolicy.aggressive();
          break;
        case 'CUSTOM':
          _customPolicy ??= _buildCustomPolicy();
          policy = _customPolicy!;
          break;
        default:
          policy = PerformancePolicy.defaults();
      }
      await _controller!.setPerformancePolicy(policy);
      await _controller!.setDataSaverMode(mode);
      setState(() {
        _dataSaverMode = mode;
      });
    }
  }

  void _downloadOfflineRegion() async {
    if (_controller == null || _isDownloading) return;
    setState(() {
      _isDownloading = true;
      _downloadStatus = 'Baixando...';
    });
    await _controller!.downloadRegion(
      region: 'LuandaCentro',
      north: -8.810,
      east: 13.235,
      south: -8.820,
      west: 13.225,
    );
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
                          const SizedBox(height: 8),
                          Text('Modo de economia de dados: $_dataSaverMode', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                          if (_downloadStatus.isNotEmpty)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4.0),
                              child: Text('Status do download offline: $_downloadStatus', style: TextStyle(color: Colors.green[800], fontWeight: FontWeight.bold)),
                            ),
                          Padding(
                            padding: const EdgeInsets.symmetric(vertical: 8.0),
                            child: Text(
                              _infoMessage,
                              style: TextStyle(color: Colors.orange[800], fontWeight: FontWeight.bold),
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
                            child: Text(
                              'Dicas de economia de dados:\n'
                              '- Use o modo AGRESSIVE para máxima economia: menos eventos, menos atualizações.\n'
                              '- O modo BALANCED reduz eventos, mas mantém boa experiência.\n'
                              '- Downloads offline só baixam uma vez por região.\n'
                              '\n'
                              'Exemplo prático:\n'
                              '1. Selecione AGRESSIVE e navegue: veja menos atualizações e menos consumo.\n'
                              '2. Tente baixar a mesma região duas vezes: só baixa uma vez.\n'
                              '3. Volte para OFF para experiência completa, sem restrições.',
                              style: TextStyle(color: Colors.blueGrey[700], fontSize: 13),
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(vertical: 4.0),
                            child: Row(
                              children: [
                                ElevatedButton(
                                  onPressed: _isDownloading ? null : _downloadOfflineRegion,
                                  child: const Text('Baixar região offline'),
                                ),
                                const SizedBox(width: 8),
                                ElevatedButton(
                                  onPressed: () => _setDataSaverMode('CUSTOM'),
                                  child: const Text('Custom Policy'),
                                ),
                              ],
                            ),
                          ),
                          Row(
                            children: [
                              ElevatedButton(
                                onPressed: () => _setDataSaverMode('OFF'),
                                child: const Text('OFF'),
                              ),
                              const SizedBox(width: 8),
                              ElevatedButton(
                                onPressed: () => _setDataSaverMode('BALANCED'),
                                child: const Text('BALANCED'),
                              ),
                              const SizedBox(width: 8),
                              ElevatedButton(
                                onPressed: () => _setDataSaverMode('AGGRESSIVE'),
                                child: const Text('AGGRESSIVE'),
                              ),
                            ],
                          ),
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