package com.github.skjolber.mockito.soap;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.*;
import javax.net.ServerSocketFactory;

/**
 * Manages one or more named ports including the allocation and destruction
 * of an arbitrary data object associated with the port.
 *
 * @param <T> the data object type
 */
abstract class PortManager<T> {

	static final int PORT_RANGE_MAX = 65535;

	/**
	 * Encapsulates a single port and its associated data.
	 */
	class Port {

		private final String name;
		private int port = -1;
		private T data;

		public Port(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int getPort() {
			return port;
		}

		public T getData() {
			return data;
		}

		public void reserve(int candidatePort, T data) {
			this.port = candidatePort;
			this.data = data;
			System.setProperty(name, Integer.toString(candidatePort));
		}

		public void release() {
			if(data != null) {
				PortManager.this.release(data);
				System.clearProperty(name);
				port = -1;
				data = null;
			}
		}

	}

	private int portRangeStart;
	private int portRangeEnd;
	private List<Port> ports = new ArrayList<>();

	/**
	 * Constructs a port manager whose ports will be allocated in the given port range.
	 *
	 * @param portRangeStart the first port number in the range
	 * @param portRangeEnd the last port number in the range
	 */
	public PortManager(int portRangeStart, int portRangeEnd) {
		if(portRangeStart <= 0) {
			throw new IllegalArgumentException("Port range start must be greater than 0.");
		}
		if(portRangeEnd < portRangeStart) {
			throw new IllegalArgumentException("Port range end must not be lower than port range end.");
		}
		if(portRangeEnd > PORT_RANGE_MAX) {
			throw new IllegalArgumentException("Port range end must not be larger than " + PORT_RANGE_MAX + ".");
		}
		this.portRangeStart = portRangeStart;
		this.portRangeEnd = portRangeEnd;
	}

	public static boolean isPortAvailable(int port) {
		try (ServerSocket serverSocket = ServerSocketFactory.getDefault()
				.createServerSocket(port, 1, InetAddress.getByName("localhost"))) {
			serverSocket.setReuseAddress(true);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Adds named ports to reserve.
	 *
	 * @param names the names associated with the ports to b reserved
	 */
	public void add(String... names) {
		if(names != null) {
			int total = ports.size() + names.length;
			if(total > (portRangeEnd - portRangeStart + 1)) {
				throw new IllegalArgumentException(
					"Cannot reserve " + names.length + " in range " + portRangeStart + "-" + portRangeEnd + ".");
			}
			for(String name : names) {
				ports.add(new Port(name));
			}
		}
	}

	/**
	 * Returns all port names and respective port numbers.
	 *
	 * @return a map of port name and port value (a valid port number
	 *         if the port has been reserved, or -1 otherwise)
	 */
	public Map<String, Integer> getPorts() {
		HashMap<String, Integer> ports = new HashMap<>();
		for (Port reservation : this.ports) {
			ports.put(reservation.getName(), reservation.getPort());
		}
		return ports;
	}

	/**
	 * Returns the port number that was reserved for the given name.
	 *
	 * @param name port name
	 * @return a valid port number if the port has been reserved, -1 otherwise
	 */
	public int getPort(String name) {
		for (Port reservation : ports) {
			if(name.equals(reservation.getName())) {
				return reservation.getPort();
			}
		}
		throw new IllegalArgumentException("No reserved port for '" + name + "'.");
	}

	/**
	 * Returns the data object associated with the given port.
	 *
	 * @param port the port number whose data is returned
	 * @return the data object associated with the given port,
	 *         or null if the given port has not been reserved
	 */
	public T getData(int port) {
		for(Port reservation : ports) {
			if(reservation.getPort() == port) {
				return reservation.getData();
			}
		}
		return null;
	}

	/**
	 * Attempts to find and reserve a free port in the required range.
	 *
	 * @param port the port object to which the reserved port and
	 *        associated data object will be assigned
	 */
	public void reserve(Port port) {
		// systematically try ports in range
		// starting at random offset
		int portRange = portRangeEnd - portRangeStart + 1;

		int offset = new Random().nextInt(portRange);

		for(int i = 0; i < portRange; i++) {
			try {
				int candidatePort = portRangeStart + (offset + portRange) % portRange;

				if(isPortAvailable(candidatePort)) {
					T data = reserve(candidatePort); // port might now be taken
					port.reserve(candidatePort, data);

					return;
				}
			} catch(Exception e) {
				// continue
			}
		}
		throw new RuntimeException("Unable to reserve port for " + port.getName());
	}

	/**
	 * Reserves all ports.
	 */
	public void start() {
		ports.forEach(this::reserve);
	}

	/**
	 * Releases all reserved ports.
	 */
	public void stop() {
		ports.forEach(Port::release);
	}

	/**
	 * Attempts to reserve the given port, returning its associated data object.
	 * Any thrown exception is considered a reservation failure.
	 *
	 * @param port the port to reserve
	 * @return the data object associated with the successfully reserved port
	 * @throws Exception if the port cannot be reserved
	 */
	public abstract T reserve(int port) throws Exception;

	/**
	 * Releases the data object associated with a port.
	 * When this method returns, the port is assumed to be available for reuse.
	 *
	 * @param data the data object to release
	 */
	public abstract void release(T data);

}
