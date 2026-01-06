enum CallState {
  initiating,
  incoming,
  ringing,
  outgoing,
  connected,
  disconnected,
  muted,
  hold,
  unknown,
}

extension CallStateParser on CallState {
  static CallState fromString(String state) {
    switch (state.toUpperCase()) {
      case 'INITIATING':
        return CallState.initiating;

      case 'INCOMING':
        return CallState.incoming;
      case 'RINGING':
        return CallState.ringing;

      case 'DIALING':
      case 'CONNECTING':
        return CallState.outgoing;

      case 'ACTIVE':
      case 'CONNECTED':
        return CallState.connected;

      case 'DISCONNECTED':
      case 'DECLINED':
        return CallState.disconnected;

      case 'HOLDING':
        return CallState.hold;

      default:
        return CallState.unknown;
    }
  }
}
