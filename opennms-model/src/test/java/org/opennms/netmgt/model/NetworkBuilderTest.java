/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opennms.core.utils.InetAddressUtils.addr;

import org.junit.Test;
import org.opennms.netmgt.model.NetworkBuilder.InterfaceBuilder;
import org.opennms.netmgt.model.OnmsNode.NodeType;

public class NetworkBuilderTest {
    @Test
    public void testMultipleIpInterfacesWithOneIfIndex() {
        final NetworkBuilder nb = new NetworkBuilder();
        nb.addNode("test").setForeignId("foo").setForeignSource("bar");
        final OnmsSnmpInterface iface = nb.addSnmpInterface(1).getSnmpInterface();
        nb.addInterface("192.168.0.1", iface).setIsManaged("M").setIsSnmpPrimary("P");
        nb.addInterface("192.168.1.1", iface).setIsManaged("M").setIsSnmpPrimary("S");

        final OnmsNode node = nb.getCurrentNode();

        assertEquals("test", node.getLabel());
        assertEquals("foo", node.getForeignId());
        assertEquals(2, node.getIpInterfaces().size());
        assertEquals(1, node.getSnmpInterfaces().size());
    }

    @Test
    public void testAddSnmpToMultipleIp() {
        final NetworkBuilder builder = new NetworkBuilder();
        builder.addNode("foo");

        final SnmpInterfaceBuilder sib = builder.addSnmpInterface(2)
            .setCollectionEnabled(true)
            .setIfOperStatus(1)
            .setIfSpeed(10000000)
            .setIfName("eth0")
            .setIfType(6);
        sib.addIpInterface("192.168.1.2").setIsManaged("M").setIsSnmpPrimary("S");
        sib.addIpInterface("192.168.1.3").setIsManaged("M").setIsSnmpPrimary("N");

        final OnmsNode node = builder.getCurrentNode();
        assertEquals("foo", node.getLabel());
        assertEquals(2, node.getIpInterfaces().size());
        assertEquals(1, node.getSnmpInterfaces().size());
    }
    
    @Test
    public void testIpInterface() {
        final NetworkBuilder builder = new NetworkBuilder();
        builder.addNode("node1");
        final InterfaceBuilder ib = builder.addSnmpInterface(1)
            .addIpInterface("192.168.1.1");
        final OnmsIpInterface iface = ib.getInterface();
        assertEquals(1, iface.getIfIndex().intValue());
    }

    @Test
    public void testDuplicateServiceAndCategoryNames() {
        final NetworkBuilder builder = new NetworkBuilder();
        builder.addNode("node1").setForeignSource("imported:").setForeignId("1").setType(NodeType.ACTIVE);
        builder.addCategory("DEV_AC");
        builder.addCategory("IMP_mid");
        builder.addCategory("OPS_Online");
        builder.addCategory("Routers"); 
        builder.setBuilding("HQ");
        builder.addSnmpInterface(1)
            .setCollectionEnabled(true)
            .setIfOperStatus(1)
            .setIfSpeed(10000000)
            .setIfDescr("ATM0")
            .setIfName("atm0")
            .setIfAlias("Initial ifAlias value")
            .setIfType(37)
            .addIpInterface("192.168.1.1").setIsManaged("M").setIsSnmpPrimary("P");
        builder.addService("ICMP");
        builder.addService("SNMP");
        builder.addSnmpInterface(2)
            .setCollectionEnabled(true)
            .setIfOperStatus(1)
            .setIfSpeed(10000000)
            .setIfName("eth0")
            .setIfType(6)
            .addIpInterface("192.168.1.2").setIsManaged("M").setIsSnmpPrimary("S");
        builder.addService("ICMP");
        builder.addService("HTTP");
        builder.addSnmpInterface(3)
            .setCollectionEnabled(false)
            .setIfOperStatus(1)
            .setIfSpeed(10000000)
            .addIpInterface("192.168.1.3").setIsManaged("M").setIsSnmpPrimary("N");
        builder.addService("ICMP");
        builder.addSnmpInterface(4)
            .setCollectionEnabled(false)
            .setIfOperStatus(1)
            .setIfSpeed(10000000)
            .addIpInterface("fe80:0000:0000:0000:aaaa:bbbb:cccc:dddd%5").setIsManaged("M").setIsSnmpPrimary("N");
        builder.addService("ICMP");
        final OnmsNode node1 = builder.getCurrentNode();
        
        assertEquals("node1", node1.getLabel());
        assertEquals("imported:", node1.getForeignSource());
        assertEquals("1", node1.getForeignId());
        assertEquals(NodeType.ACTIVE, node1.getType());
        //assertEquals("127.0.0.1", node1.getDistPoller().getIpAddress());
        assertNull(node1.getSysContact());
        assertEquals(4, node1.getSnmpInterfaces().size());
        assertEquals(4, node1.getIpInterfaces().size());
        
        final OnmsIpInterface two = node1.getIpInterfaceByIpAddress("192.168.1.2");
        assertNotNull(two);
        final OnmsServiceType twoIcmp = two.getMonitoredServiceByServiceType("ICMP").getServiceType();
        assertNotNull(twoIcmp);

        final OnmsIpInterface three = node1.getIpInterfaceByIpAddress("192.168.1.3");
        assertNotNull(three);
        final OnmsServiceType threeIcmp = three.getMonitoredServiceByServiceType("ICMP").getServiceType();
        assertNotNull(threeIcmp);

        //assertEquals(twoIcmp, threeIcmp);
        //assertEquals(twoIcmp.getId(), threeIcmp.getId());
        //assertEquals(twoIcmp.hashCode(), threeIcmp.hashCode());
        
        final OnmsMonitoredService svc = two.getMonitoredServiceByServiceType("ICMP");
        assertEquals(addr("192.168.1.2"), svc.getIpAddress());
        assertEquals("ICMP", svc.getServiceName());
        assertEquals(2, svc.getIfIndex().intValue());

        builder.addNode("node2").setForeignSource("imported:").setForeignId("2").setType(NodeType.ACTIVE);
        builder.addCategory("IMP_mid");
        builder.addCategory("Servers");
        builder.setBuilding("HQ");
        builder.addInterface("192.168.2.1").setIsManaged("M").setIsSnmpPrimary("P");
        builder.addService("ICMP");
        builder.addService("SNMP");
        builder.addInterface("192.168.2.2").setIsManaged("M").setIsSnmpPrimary("S");
        builder.addService("ICMP");
        builder.addService("HTTP");
        builder.addInterface("192.168.2.3").setIsManaged("M").setIsSnmpPrimary("N");
        builder.addService("ICMP");
        final OnmsNode node2 = builder.getCurrentNode();
        
        assertNotNull(node2);
        assertEquals("node2", node2.getLabel());
        final OnmsIpInterface twoTwo = node2.getIpInterfaceByIpAddress("192.168.2.2");
        assertNotNull(twoTwo);
        final OnmsServiceType twoTwoIcmp = twoTwo.getMonitoredServiceByServiceType("ICMP").getServiceType();
        assertEquals(twoTwoIcmp.getName(), twoIcmp.getName());
        
        final OnmsCategory catNode1 = getCategory("IMP_mid", node1);
        final OnmsCategory catNode2 = getCategory("IMP_mid", node2);
        assertEquals(catNode1, catNode2);
        assertEquals(catNode1.getId(), catNode2.getId());
        assertEquals(catNode1.hashCode(), catNode2.hashCode());
    }

    private OnmsCategory getCategory(final String catName, final OnmsNode node) {
        for (final OnmsCategory cat : node.getCategories()) {
            if (catName.equals(cat.getName())) {
                return cat;
            }
        }
        return null;
    }
}
