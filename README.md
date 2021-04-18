# Distributed System for File Sharing
Simple overlay-network based distributed system that allows a set of nodes to share contents among each other

## User Instructions

1. Java should be installed in the machine to compile and run the code.
2. Start the server and nodes in a distributed environment as in [Getting Started](#getting-started).
3. After starting the server and nodes, you can use the app for using the network.
4. If you need fine-grained access to the system, you can use [Netcat Commands](#netcat-commands).

## Getting Started
1. Clone the source code to every machine which will be connected to the distributed network.
2. Select a machine as the server and run following commands to start as the server.
    1. `cd distributed-system/src`
    2. `javac server/Server.java`
    3. `java server.Server -port=<port>`
>Note: -port flag is optional and default port is `55555`.
3. In a machine that is selected as a node, run following commands to start as a node.
    1. `cd distributed-system/src`
    2. `javac node/Node.java`
    3. `java node.Node -port=<port> -server=<ip>:<port>`
>Note: -port flag is optional and default port is `55556`. If server is not configured `localhost:55555` will be taken as default.
4. Now you can start the app in the same node or in a different machine to control a given node. 
    1. `cd distributed-system/src`
    2. `javac app/App.java`
    3. `java app.App -node=<ip>:<port>`
>Note: If node is not configured `localhost:55556` will be taken as default.

## Netcat Commands

<table>
    <thead>
        <tr>
            <th>Component</th>
            <th>Task</th>
            <th>Request Syntax</th>
            <th>Response Syntax</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td rowspan="3">Server</td>
            <td>Register a node</td>
            <td><code>&lt;length> REG &lt;ip> &lt;port> &lt;username></code></td>
            <td><code>&lt;length> REGOK &lt;#nodes> &lt;ip1> &lt;port1> &lt;ip2> &lt;port2>...</code></td>
        </tr>
        <tr>
            <td>Unregister a node</td>
            <td><code>&lt;length> UNREG &lt;ip> &lt;port> &lt;username></code></td>
            <td><code>&lt;length> UNROK &lt;value></code></td>
        </tr>
        <tr>
            <td>Print registered nodes</td>
            <td><code>&lt;length> PRINT</code></td>
            <td><code>&lt;length> PRINTOK &lt;#nodes> &lt;ip1> &lt;port1> &lt;username1> &lt;ip2> &lt;port2> &lt;username2>...</code></td>
        </tr>
        <tr>
            <td rowspan="8">Node</td>
            <td>Join the network</td>
            <td><code>&lt;length> JOIN &lt;ip> &lt;port></code></td>
            <td><code>&lt;length> JOINOK &lt;value></code></td>
        </tr>
        <tr>
            <td>Leave the network</td>
            <td><code>&lt;length> LEAVE &lt;ip> &lt;port></code></td>
            <td><code>&lt;length> LEAVEOK &lt;value></code></td>
        </tr>
        <tr>
            <td>Search files using a search term</td>
            <td><code>&lt;length> SER &lt;ip> &lt;port> “&lt;searchterm>” &lt;hops></code></td>
            <td><code>&lt;length> SEROK &lt;#files> &lt;ip> &lt;port> &lt;hops> “&lt;filename1>” “&lt;filename2>”...</code></td>
        </tr>
        <tr>
            <td>Download a given file</td>
            <td><code>&lt;length> DOWNLOAD &lt;ip> &lt;port> “&lt;filename>”</code></td>
            <td><code>&lt;length> DOWNLOADOK &lt;value></code></td>
        </tr>
        <tr>
            <td>Start the node</td>
            <td><code>&lt;length> START</code></td>
            <td><code>&lt;length> STARTOK &lt;value></code></td>
        </tr>
        <tr>
            <td>Stop the node</td>
            <td><code>&lt;length> STOP</code></td>
            <td><code>&lt;length> STOPOK &lt;value></code></td>
        </tr>
        <tr>
            <td>Print nodes in the routing table</td>
            <td><code>&lt;length> PRINT</code></td>
            <td><code>&lt;length> PRINTOK &lt;#nodes> &lt;ip1> &lt;port1> &lt;ip2> &lt;port2>...</code></td>
        </tr>
        <tr>
            <td>Print files in current node</td>
            <td><code>&lt;length> PRINTF</code></td>
            <td><code>&lt;length> PRINTFOK &lt;#files> &lt;filename1> &lt;filename2>...</code></td>
        </tr>
    </tbody>
</table>

### Responses with error codes

<table>
    <thead>
        <tr>
            <th>Response Type</th>
            <th>Error Code</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td rowspan=4>REGOK</td>
            <td>9999</td>
            <td>There is some error in the command.</td>
        </tr>
        <tr>
            <td>9998</td>
            <td>Already registered to you. Unregister first.</td>
        </tr>
        <tr>
            <td>9997</td>
            <td>Registered to another user. Try a different IP or port.</td>
        </tr>
        <tr>
            <td>9996</td>
            <td>Can’t register because server is full.</td>
        </tr>
        <tr>
            <td>UNROK</td>
            <td>9999</td>
            <td>IP and port may not be in the registry or command is incorrect.</td>
        </tr>
        <tr>
            <td>JOINOK</td>
            <td>9999</td>
            <td>Error while adding the new node to the routing table.</td>
        </tr>
        <tr>
            <td>LEAVEOK</td>
            <td>9999</td>
            <td>Error while removing the node from the routing table.</td>
        </tr>
        <tr>
            <td rowspan="2">SEROK</td>
            <td>9999</td>
            <td>Failure due to unreachable nodes.</td>
        </tr>
        <tr>
            <td>9998</td>
            <td>Failure due to some other error.</td>
        </tr>
        <tr>
            <td>STARTOK</td>
            <td>9999</td>
            <td>Error while registering and joining the network.</td>
        </tr>
        <tr>
            <td>STOPOK</td>
            <td>9999</td>
            <td>Error while unregistering and leaving the network.</td>
        </tr>
    </tbody>
</table>



