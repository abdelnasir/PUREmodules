#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include "config.h"
#include "nrf_drv_twi.h"
#include "nrf_gpio.h"
#include "app_error.h"
#include "nrf.h"
#include "bsp.h"
#include "app_util_platform.h"
#define NRF_LOG_MODULE_NAME "APP"
#include "nrf_log.h"
#include "nrf_log_ctrl.h"
#include "nrf_delay.h"
#include "ble_nus.h"


#include "i2c_driver.h"
#include "ble_driver.h"
#include "lis3mdl.h"
#include "lis2de.h"
#include "vl53l0.h"
#include "si1153.h"
#include "veml6075.h"
#include "bme280.h"
#include "apds9250.h"
#include "supersensor.h"
#include "p1234701ct.h"

#include "nrf_drv_timer.h"


const nrf_drv_timer_t TIMER_DATA = NRF_DRV_TIMER_INSTANCE(0);

/**
 * @brief TWI master instance
 *
 * Instance of TWI master driver that would be used for communication with simulated
 * eeprom memory.
 */
static const nrf_drv_twi_t m_twi_master = NRF_DRV_TWI_INSTANCE(MASTER_TWI_INST);

/**
 * @brief Initialize the master TWI
 *
 * Function used to initialize master TWI interface that would communicate with simulated EEPROM.
 *
 * @return NRF_SUCCESS or the reason of failure
 */
static ret_code_t twi_master_init(void)
{
    ret_code_t ret;
    const nrf_drv_twi_config_t config =
    {
       .scl                = TWI_SCL_M,
       .sda                = TWI_SDA_M,
       .frequency          = NRF_TWI_FREQ_400K,
       .interrupt_priority = APP_IRQ_PRIORITY_HIGH,
       .clear_bus_init     = false
    };

    ret = nrf_drv_twi_init(&m_twi_master, &config, NULL, NULL);

    if (NRF_SUCCESS == ret)
    {
        nrf_drv_twi_enable(&m_twi_master);
    }

    return ret;
}

/**
 * @brief Handler for timer events.
 */
void timer_event_handler(nrf_timer_event_t event_type, void* p_context)
{
    static uint32_t i;
    uint32_t led_to_invert = ((i++) % LEDS_NUMBER);

    switch (event_type)
    {
        case NRF_TIMER_EVENT_COMPARE0:
            bsp_board_led_invert(led_to_invert);
            NRF_LOG_RAW_INFO("\r\nTimer Hit %d \r\n",i);
            //run_lis2de(m_twi_master);
            break;

        default:
            //Do nothing.
            NRF_LOG_RAW_INFO("\r\nTimer Hit inside %d \r\n",i);

            break;
    }
}

static int twi_device_search(void)
{
	int i = 0;
	uint8_t buffer[1]; 
	ret_code_t ret;

	for(i=1;i<128;i++)
	{
		NRF_LOG_RAW_INFO("CHECKING 0x%x, ",i);
		NRF_LOG_FLUSH();   
		ret = nrf_drv_twi_rx(&m_twi_master, i, buffer, 1);
		if (NRF_SUCCESS != ret){
			//NRF_LOG_WARNING("Communication error when Writing\r\n");
		} 
		else
		{
			NRF_LOG_RAW_INFO("\n\rDEVICE FOUND at ADDRESS 0x%x \r\n",i);

		}
		nrf_delay_ms(10);
	}

	 return 1;
}

/**
 *  The begin of the journey
 */
static int p12347_test_init(void)
{
	int i = 0;
	ret_code_t err_code;
	/* Initialization of UART */
	bsp_board_leds_init();

	APP_ERROR_CHECK(NRF_LOG_INIT(NULL));

	/* Initializing TWI master interface for EEPROM */
	err_code = twi_master_init();
	APP_ERROR_CHECK(err_code);

	twi_device_search();

	NRF_LOG_RAW_INFO("lis2de_init start\n\r");
	NRF_LOG_FLUSH();   
	lis2de_init(m_twi_master);
	lis2de_pass(m_twi_master);
	nrf_delay_ms(10);

	NRF_LOG_RAW_INFO("p12347_test start\n\r");
	NRF_LOG_FLUSH();   

	p1234701ct_pass(m_twi_master);
	NRF_LOG_RAW_INFO("p12347_test complete\n\r");
	NRF_LOG_FLUSH();   

}

/**
 *  The begin of the journey
 */
int main(void)
{
	bool p1234701ct_status; 
    p12347_test_init();

    p1234701ct_status = p1234701ct_pass(m_twi_master);;
    p1234701ct_init(m_twi_master);
    NRF_LOG_FLUSH();   
    while(p1234701ct_status)
    {
	    run_p1234701ct(m_twi_master);
	    NRF_LOG_FLUSH();   
	    nrf_delay_ms(10);
    }

    
    NRF_LOG_FLUSH();   
}
