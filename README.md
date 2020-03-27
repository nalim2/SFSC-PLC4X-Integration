# Welcome to the PLC4X integration service for SFSC

The service implemented here will include the READ, WRITE and CONTINIUS-READ/SUBSCRIBE functions of machine controllers via their automation interface using the Apache project PLC4X. These services are implemented as SFSC services that require an SFSC core module to run as a local or remote broker. 

>Caution, the services presented here are under development and will change in structure and behaviour.

# Configuration
The services are configured via local environment variables that are displayed in the docker file. In the following the possible configuration values with their data type and default value are presented.
|Variable          |Type                 |Default  |Description                            |
|------------------|---------------------|---------|---------------------------------------|
|EXTERNEL_CORE_IP  |Hostname / IP-Address|127.0.0.1|The TCP IP or hostname of the SFSC core|
|EXTERNEL_CORE_PORT|TCP-Port: 1 - 65535  |1251     |THE TCP port of the SFSC core          |





# Services
Here we briefly introduce the three different services READ, WRITE and CONTINIUS-READ/SUBSCRIBE, which are to be implemented in this repository. Each of these services will be included in the Docker image. By default, the write service will be disabled.
## Monitoring over PLC4X scrapper api
### Example
## Read plc values over PLC4X
### Example
## Write plc values over PLC4X
Not implemented yet
