
__kernel void convolve_image_2d(
        __read_only image2d_t input,
        __read_only image2d_t filterkernel,
        __write_only image2d_t output,
        __private int radius
)
{
    const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

    int2 pos = {get_global_id(0), get_global_id(1)};

    float sum = 0.0f;

    for(int x = -radius; x < radius + 1; x++)
    {
        for(int y = -radius; y < radius + 1; y++)
        {
            const int2 kernelPos = {x+radius, y+radius};
            sum += read_imagef(filterkernel, sampler, kernelPos).x
                 * read_imagef(input, sampler, pos + (int2)( x, y )).x;
        }
    }

    float4 pix = {sum,0,0,0};
	write_imagef(output, pos, pix);
}


__kernel void convolve_image_2d_separated(
        __read_only image2d_t input,
        __read_only image2d_t filterkernel,
        __write_only image2d_t output,
        __private int radius,
        __private int dim
)
{
    const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

    const int2 dir   = (int2)(dim==0,dim==1);

    int2 pos = {get_global_id(0), get_global_id(1)};

    float sum = 0.0f;

    for(int x = -radius; x < radius + 1; x++)
    {
        const int2 kernelPos = (int2){radius, radius} + x * dir;
        sum += read_imagef(filterkernel, sampler, kernelPos).x
             * read_imagef(input, sampler, pos + x * dir).x;

    }

    float4 pix = {sum,0,0,0};
	write_imagef(output, pos, pix);
}




__kernel void subtract_convolved_images_2d(
        __read_only image2d_t input,
        __read_only image2d_t filterkernel_minuend,
        __read_only image2d_t filterkernel_subtrahend,
        __write_only image2d_t output,
        __private int radius
)
{
    const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

    int2 pos = {get_global_id(0), get_global_id(1)};

    float sum_minuend = 0.0f;
    float sum_subtrahend = 0.0f;

    for(int x = -radius; x < radius + 1; x++)
    {
        for(int y = -radius; y < radius + 1; y++)
        {
            const int2 kernelPos = {x+radius, y+radius};

            float image_pixel_value = read_imagef(input, sampler, pos + (int2)( x, y )).x;

            sum_minuend += read_imagef(filterkernel_minuend, sampler, kernelPos).x
                 * image_pixel_value;
            sum_subtrahend += read_imagef(filterkernel_subtrahend, sampler, kernelPos).x
                 * image_pixel_value;
        }
    }

    float4 pix = {sum_minuend - sum_subtrahend,0,0,0};
	write_imagef(output, pos, pix);
}


__kernel void subtract_convolved_images_2d_fast(
        __read_only image2d_t input,
        __write_only image2d_t output,
        __private int radius,
        __private float sigma_minuend,
        __private float sigma_subtrahend
)
{
    const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

    int2 pos = {get_global_id(0), get_global_id(1)};

    float sum_minuend = 0.0f;
    float sum_subtrahend = 0.0f;
    float weighted_sum_minuend = 0.0f;
    float weighted_sum_subtrahend = 0.0f;

    float sigma_minuend_squared_twice = 2.0f * pow(sigma_minuend, 2);
    float sigma_subtrahend_squared_twice = 2.0f * pow(sigma_subtrahend, 2);

    for(int x = -radius; x < radius + 1; x++)
    {
        float xSquared = pow((float)x,2);
        for(int y = -radius; y < radius + 1; y++)
        {
            float ySquared = pow((float)y,2);
            const int2 kernelPos = {x+radius, y+radius};

            float image_pixel_value = read_imagef(input, sampler, pos + (int2)( x, y )).x;

            float weight_minuend = exp(-((xSquared + ySquared) / sigma_minuend_squared_twice));
            float weight_subtrahend = exp(-((float) (xSquared + ySquared) / sigma_subtrahend_squared_twice));

            weighted_sum_minuend += weight_minuend * image_pixel_value;
            weighted_sum_subtrahend += weight_subtrahend * image_pixel_value;

            sum_minuend += weight_minuend;
            sum_subtrahend += weight_subtrahend;
        }
    }

    float4 pix = {weighted_sum_minuend / sum_minuend - weighted_sum_subtrahend / sum_subtrahend,0,0,0};
	write_imagef(output, pos, pix);
}
