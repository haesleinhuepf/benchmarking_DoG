
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
